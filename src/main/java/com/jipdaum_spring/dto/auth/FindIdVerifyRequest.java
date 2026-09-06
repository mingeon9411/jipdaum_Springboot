package com.jipdaum_spring.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record FindIdVerifyRequest(
        @NotBlank(message = "이메일을 입력해주세요.") String email,
        @NotBlank(message = "인증 코드를 입력해주세요.") String code,
        @JsonProperty("security_answer") @NotBlank(message = "보안 질문의 답변을 입력해주세요.") String securityAnswer
) {
}
