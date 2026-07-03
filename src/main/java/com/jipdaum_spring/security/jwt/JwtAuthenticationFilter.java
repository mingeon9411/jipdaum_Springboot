package com.jipdaum_spring.security.jwt;

import com.jipdaum_spring.domain.springuser.User;
import com.jipdaum_spring.domain.springuser.UserRepository;
import com.jipdaum_spring.domain.jipdaumuser.JipdaumUserRepository;
import com.jipdaum_spring.security.CustomUserDetails;
import com.jipdaum_spring.security.JipdaumUserProvisioner;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** 만료된 access token이 들어온 요청에 표시해두는 request attribute 키. */
    public static final String TOKEN_EXPIRED_ATTRIBUTE = "jwtExpired";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final JipdaumUserRepository jipdaumUserRepository;
    private final JipdaumUserProvisioner jipdaumUserProvisioner;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);
        JwtTokenProvider.TokenStatus status = null;
        if (token != null) {
            // refresh token은 재발급 전용이라 API 인증에 쓸 수 없다. type 클레임이 아예 없는
            // 레거시(Django) 토큰은 계속 통과시켜 기존 호환성을 유지한다.
            if (JwtTokenProvider.TYPE_REFRESH.equals(jwtTokenProvider.getType(token))) {
                status = JwtTokenProvider.TokenStatus.INVALID;
            } else {
                status = jwtTokenProvider.validateToken(token);
            }
        }

        if (status == JwtTokenProvider.TokenStatus.EXPIRED) {
            // permitAll 경로(예: 상품 조회)는 그대로 익명으로 통과시키되,
            // 로그인이 필요한 서비스 로직(CurrentUserProvider)이 "만료됨"을 구분할 수 있도록 표시만 남긴다.
            request.setAttribute(TOKEN_EXPIRED_ATTRIBUTE, Boolean.TRUE);
        } else if (status == JwtTokenProvider.TokenStatus.VALID) {
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
                String resolvedEmail = email;
                Optional<User> springUser = userRepository.findByEmail(email);
                User authUser = springUser.orElseGet(() ->
                        User.builder().email(resolvedEmail).role(User.Role.USER).build());

                // 소셜 로그인 유저가 JIPDAUM_USER에 없으면 즉시 생성 (기존 세션 토큰 호환)
                springUser.ifPresent(u -> jipdaumUserProvisioner.ensureExists(
                        resolvedEmail, resolvedEmail, u.getName() != null ? u.getName() : "소셜사용자"));

                CustomUserDetails principal = new CustomUserDetails(authUser, email);
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities()
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
