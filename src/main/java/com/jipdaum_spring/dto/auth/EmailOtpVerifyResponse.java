package com.jipdaum_spring.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

/** hasSecurityQuestion: 로그인 완료 직후 보안질문 설정 안내를 띄울지 프론트가 판단하는 값. */
public record EmailOtpVerifyResponse(
        String message,
        @JsonProperty("has_security_question") boolean hasSecurityQuestion
) {
}
