package com.jipdaum_spring.security;

import com.jipdaum_spring.domain.jipdaumuser.JipdaumUser;
import com.jipdaum_spring.domain.jipdaumuser.JipdaumUserRepository;
import com.jipdaum_spring.security.jwt.JwtAuthenticationFilter;
import com.jipdaum_spring.security.jwt.TokenExpiredException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * 로그인이 필요한 서비스 로직에서 현재 사용자를 조회하는 공통 진입점.
 * access token이 만료된 경우와 애초에 인증되지 않은 경우를 구분해서 예외를 던진다.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    private final JipdaumUserRepository jipdaumUserRepository;
    private final HttpServletRequest request;

    public JipdaumUser getCurrentUser() {
        if (Boolean.TRUE.equals(request.getAttribute(JwtAuthenticationFilter.TOKEN_EXPIRED_ATTRIBUTE))) {
            throw new TokenExpiredException();
        }

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        // 인증된 요청은 JwtAuthenticationFilter가 심어둔 CustomUserDetails, 익명 요청은 Spring Security 기본 문자열("anonymousUser")
        String email = principal instanceof CustomUserDetails cud ? cud.getUsername() : String.valueOf(principal);
        Optional<JipdaumUser> byEmail = jipdaumUserRepository.findByEmail(email);

        // principal에 @가 없으면 Spring Boot 형식 → Django 유저(이메일에 @ 포함)를 우선 탐색
        if (!email.contains("@")) {
            Optional<JipdaumUser> djangoUser = jipdaumUserRepository.findAllByUsernameIgnoreCase(email)
                    .stream()
                    .filter(u -> u.getEmail() != null && u.getEmail().contains("@"))
                    .findFirst();
            if (djangoUser.isPresent()) return djangoUser.get();
        }

        return byEmail.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));
    }
}