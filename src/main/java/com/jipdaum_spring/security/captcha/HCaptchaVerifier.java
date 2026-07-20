package com.jipdaum_spring.security.captcha;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

/**
 * Django의 verify_hcaptcha()와 동일한 정책: secret key가 설정되지 않은 환경(로컬 개발)에서는
 * 통과시키고, hCaptcha 쪽 호출이 실패하면 안전하게 실패 처리한다.
 */
@Slf4j
@Component
public class HCaptchaVerifier {

    private static final String VERIFY_URL = "https://hcaptcha.com/siteverify";

    @Value("${hcaptcha.secret-key:}")
    private String secretKey;

    private final WebClient webClient = WebClient.create();

    public boolean verify(String token) {
        if (!StringUtils.hasText(secretKey)) {
            return true;
        }
        if (!StringUtils.hasText(token)) {
            return false;
        }

        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("secret", secretKey);
            form.add("response", token);

            Map<?, ?> response = webClient.post()
                    .uri(VERIFY_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(form))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(5));

            return response != null && Boolean.TRUE.equals(response.get("success"));
        } catch (Exception e) {
            log.warn("hCaptcha 검증 호출 실패", e);
            return false;
        }
    }
}
