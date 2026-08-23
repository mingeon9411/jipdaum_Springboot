package com.jipdaum_spring.security.captcha;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 원래는 hCaptcha siteverify API로 사람인지 검증했지만, 회원가입/로그인/소셜로그인의 "사람 확인"
 * 단계를 PASS(통신사 본인인증) 데모로 교체하면서 실제 검증은 하지 않게 됐다(PassVerifyModal.jsx
 * 참고 — 통신사 계약이 필요해 실제 연동이 불가능한 개인/학생 프로젝트라 화면 흐름만 재현한다).
 * isEnabled()만 SocialLoginCaptchaFilter가 "PASS 데모 단계를 거쳐야 하는지"를 판단하는 데 남아있다.
 */
@Component
public class HCaptchaService {

    @Value("${hcaptcha.secret-key:}")
    private String secretKey;

    public boolean isEnabled() {
        return StringUtils.hasText(secretKey);
    }
}
