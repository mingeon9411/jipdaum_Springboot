package com.jipdaum_spring.dto.captcha;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * hCaptcha siteverify API(https://hcaptcha.com/siteverify) 응답.
 * score/score_reason 등 Enterprise 전용 필드는 안 쓰므로 생략 — 모르는 필드가 와도 역직렬화가
 * 깨지지 않도록 무시한다.
 * https://docs.hcaptcha.com/#server 참고.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HCaptchaResponseDto(
        boolean success,
        @JsonProperty("challenge_ts") String challengeTs,
        String hostname,
        @JsonProperty("error-codes") List<String> errorCodes
) {
}