package com.jipdaum_spring.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record EmailOtpSendRequest(
        @NotBlank(message = "이메일을 입력해주세요.") String email
) {
}
