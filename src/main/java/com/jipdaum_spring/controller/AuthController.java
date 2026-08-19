package com.jipdaum_spring.controller;

import com.jipdaum_spring.dto.auth.AccessTokenResponse;
import com.jipdaum_spring.dto.auth.RefreshTokenRequest;
import com.jipdaum_spring.dto.auth.SocialCaptchaRequest;
import com.jipdaum_spring.dto.auth.SocialCaptchaResponse;
import com.jipdaum_spring.dto.auth.SocialExchangeRequest;
import com.jipdaum_spring.dto.auth.TokenPairResponse;
import com.jipdaum_spring.exception.AuthException;
import com.jipdaum_spring.security.captcha.HCaptchaService;
import com.jipdaum_spring.security.captcha.SocialLoginCaptchaTicketStore;
import com.jipdaum_spring.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    private final HCaptchaService hCaptchaService;
    private final SocialLoginCaptchaTicketStore socialLoginCaptchaTicketStore;

    @PostMapping("/social-exchange")
    public ResponseEntity<?> socialExchange(@RequestBody SocialExchangeRequest request) {
        TokenPairResponse response = authService.exchangeSocialCode(request.code());
        return ResponseEntity.ok(response);
    }

    /**
     * 소셜 로그인(카카오/네이버/구글) 버튼은 브라우저가 /oauth2/authorization/{id}로 직접 이동하는
     * 방식이라 JSON 바디에 캡차 토큰을 실을 수 없다. 프론트는 버튼 클릭 시 이 엔드포인트를 먼저 호출해
     * hCaptcha 토큰을 검증받고, 응답받은 ticket을 /oauth2/authorization/{id}?ticket=... 에 붙여
     * 이동해야 한다(SocialLoginCaptchaFilter가 검사). ticket은 60초 내 1회만 유효하다.
     */
    @PostMapping("/social-captcha")
    public ResponseEntity<SocialCaptchaResponse> socialCaptcha(@RequestBody SocialCaptchaRequest request) {
        if (!hCaptchaService.verify(request.recaptchaToken())) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "보안 인증에 실패했습니다. 다시 시도해주세요.");
        }
        return ResponseEntity.ok(new SocialCaptchaResponse(socialLoginCaptchaTicketStore.issue()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshTokenRequest request) {
        AccessTokenResponse response = authService.refresh(request.refresh());
        return ResponseEntity.ok(response);
    }
}
