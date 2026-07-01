package com.jipdaum_spring.security.jwt;

import com.jipdaum_spring.domain.User.User;
import com.jipdaum_spring.domain.User.UserRepository;
import com.jipdaum_spring.domain.user.JipdaumUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final JipdaumUserRepository jipdaumUserRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && jwtTokenProvider.validate(token)) {
            String email = jwtTokenProvider.getEmail(token);

            // Django SimpleJWT: sub 클레임 없이 user_id 클레임(정수) 사용
            if (email == null) {
                Long userId = jwtTokenProvider.getUserId(token);
                if (userId != null) {
                    email = jipdaumUserRepository.findById(userId)
                            .map(u -> u.getEmail())
                            .orElse(null);
                }
            }

            if (email != null) {
                Optional<User> springUser = userRepository.findByEmail(email);
                String role = springUser.map(u -> "ROLE_" + u.getRole().name()).orElse("ROLE_USER");

                // 소셜 로그인 유저가 JIPDAUM_USER에 없으면 즉시 생성 (기존 세션 토큰 호환)
                // username 대소문자 무시 조회도 체크해서 Django 유저가 있으면 중복 생성 방지
                if (springUser.isPresent()
                        && jipdaumUserRepository.findByEmail(email).isEmpty()
                        && jipdaumUserRepository.findAllByUsernameIgnoreCase(email).isEmpty()) {
                    User u = springUser.get();
                    String nickname = u.getName() != null ? u.getName() : "소셜사용자";
                    try {
                        jdbcTemplate.update(
                            "INSERT INTO JIPDAUM_USER " +
                            "(username, password, last_login, is_superuser, first_name, last_name, " +
                            "email, is_staff, is_active, created_at, nickname, is_email_verified) " +
                            "VALUES (?, ?, NULL, 0, '', '', ?, 0, 1, SYSDATE, ?, 0)",
                            email, "!" + email, email, nickname
                        );
                    } catch (Exception ignored) {}
                }

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        email, null, List.of(new SimpleGrantedAuthority(role))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
