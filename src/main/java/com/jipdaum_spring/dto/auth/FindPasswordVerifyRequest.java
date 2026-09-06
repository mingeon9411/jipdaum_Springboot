package com.jipdaum_spring.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record FindPasswordVerifyRequest(
        @NotBlank(message = "닉네임을 입력해주세요.") String nickname,
        @NotBlank(message = "이메일을 입력해주세요.") String email,
        @JsonProperty("security_answer") @NotBlank(message = "보안 질문의 답변을 입력해주세요.") String securityAnswer
) {
}
