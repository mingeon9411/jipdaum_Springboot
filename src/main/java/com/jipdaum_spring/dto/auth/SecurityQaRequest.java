package com.jipdaum_spring.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record SecurityQaRequest(
        @JsonProperty("security_question") @NotBlank(message = "보안 질문을 선택해주세요.") String securityQuestion,
        @JsonProperty("security_answer") @NotBlank(message = "보안 질문의 답변을 입력해주세요.") String securityAnswer
) {
}
