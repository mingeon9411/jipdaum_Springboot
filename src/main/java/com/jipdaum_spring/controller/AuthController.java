package com.jipdaum_spring.controller;

import com.jipdaum_spring.dto.auth.AccessTokenResponse;
import com.jipdaum_spring.dto.auth.RefreshTokenRequest;
import com.jipdaum_spring.dto.auth.SocialCaptchaRequest;
import com.jipdaum_spring.dto.auth.SocialCaptchaResponse;
import com.jipdaum_spring.dto.auth.SocialExchangeRequest;
import com.jipdaum_spring.dto.auth.TokenPairResponse;
import com.jipdaum_spring.security.captcha.SocialLoginCaptchaTicketStore;
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
    private final SocialLoginCaptchaTicketStore socialLoginCaptchaTicketStore;

    @PostMapping("/social-exchange")
    public ResponseEntity<?> socialExchange(@RequestBody SocialExchangeRequest request) {
        TokenPairResponse response = authService.exchangeSocialCode(request.code());
        return ResponseEntity.ok(response);
    }

    /**
     * 소셜 로그인(카카오/네이버/구글) 버튼은 브라우저가 /oauth2/authorization/{id}로 직접 이동하는
     * 방식이라 JSON 바디에 PASS 토큰을 실을 수 없다. 프론트는 버튼 클릭 시 이 엔드포인트를 먼저
     * 호출해 ticket을 발급받고, /oauth2/authorization/{id}?ticket=... 에 붙여 이동해야 한다
     * (SocialLoginCaptchaFilter가 검사). ticket은 60초 내 1회만 유효하다.
     *
     * PASS(통신사 본인인증) 데모는 실제 검증을 하지 않는다(HCaptchaService 참고) — 프론트가 이
     * 엔드포인트를 호출했다는 것 자체가 PASS 흐름을 거쳤다는 뜻이라 그대로 ticket을 발급한다.
     */
    @PostMapping("/social-captcha")
    public ResponseEntity<SocialCaptchaResponse> socialCaptcha(@RequestBody SocialCaptchaRequest request) {
        return ResponseEntity.ok(new SocialCaptchaResponse(socialLoginCaptchaTicketStore.issue()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshTokenRequest request) {
        AccessTokenResponse response = authService.refresh(request.refresh());
        return ResponseEntity.ok(response);
    }
}
