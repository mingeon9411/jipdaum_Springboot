package com.jipdaum_spring.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record EmailOtpVerifyRequest(
        @NotBlank(message = "이메일을 입력해주세요.") String email,
        @NotBlank(message = "인증 코드를 입력해주세요.") String code
) {
}
