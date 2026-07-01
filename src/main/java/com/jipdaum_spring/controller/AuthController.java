package com.jipdaum_spring.controller;

import com.jipdaum_spring.domain.user.JipdaumUserRepository;
import com.jipdaum_spring.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final JipdaumUserRepository jipdaumUserRepository;

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refresh");

        if (!StringUtils.hasText(refreshToken) || !jwtTokenProvider.validate(refreshToken)) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired refresh token"));
        }

        String email = jwtTokenProvider.getEmail(refreshToken);

        // Django refresh token: sub 없이 user_id 클레임 사용
        if (email == null) {
            Long userId = jwtTokenProvider.getUserId(refreshToken);
            if (userId != null) {
                email = jipdaumUserRepository.findById(userId)
                        .map(u -> u.getEmail())
                        .orElse(null);
            }
        }

        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Cannot identify user from token"));
        }

        // Spring Boot 형식의 새 access token 발급 (sub = email)
        String newAccessToken = jwtTokenProvider.generateAccessToken(email);
        return ResponseEntity.ok(Map.of("access", newAccessToken));
    }
}