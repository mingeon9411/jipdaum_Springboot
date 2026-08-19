package com.jipdaum_spring.security.captcha;

import com.jipdaum_spring.dto.captcha.HCaptchaResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * hCaptcha siteverify API로 사람인지 검증한다. 이 앱은 서블릿 기반 blocking MVC라(Security/JPA 전부
 * non-reactive) 굳이 WebClient+block()을 쓰지 않고, 동기 클라이언트인 RestClient를 그대로 쓴다.
 *
 * Django의 verify_hcaptcha()와 동일한 정책: secret key가 설정되지 않은 환경(로컬 개발)에서는
 * 통과시키고, hCaptcha 쪽 호출이 실패하면 안전하게 실패 처리한다.
 */
@Slf4j
@Component
public class HCaptchaService {

    @Value("${hcaptcha.secret-key:}")
    private String secretKey;

    @Value("${hcaptcha.verify-url:https://hcaptcha.com/siteverify}")
    private String verifyUrl;

    private final RestClient restClient = RestClient.builder()
            .requestFactory(timeoutRequestFactory())
            .build();

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

            HCaptchaResponseDto response = restClient.post()
                    .uri(verifyUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(HCaptchaResponseDto.class);

            return response != null && response.success();
        } catch (RestClientException e) {
            log.warn("hCaptcha 검증 호출 실패", e);
            return false;
        }
    }

    private static SimpleClientHttpRequestFactory timeoutRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(5_000);
        return factory;
    }
}