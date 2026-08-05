package com.jipdaum_spring.controller;

import com.jipdaum_spring.dto.auth.AccessTokenResponse;
import com.jipdaum_spring.dto.auth.RefreshTokenRequest;
import com.jipdaum_spring.dto.auth.SocialExchangeRequest;
import com.jipdaum_spring.dto.auth.TokenPairResponse;
import com.jipdaum_spring.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/social-exchange")
    public ResponseEntity<?> socialExchange(@RequestBody SocialExchangeRequest request) {
        TokenPairResponse response = authService.exchangeSocialCode(request.code());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshTokenRequest request) {
        AccessTokenResponse response = authService.refresh(request.refresh());
        return ResponseEntity.ok(response);
    }
}
