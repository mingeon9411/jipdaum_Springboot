package com.jipdaum_spring.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @JsonProperty("reset_token") @NotBlank(message = "본인확인 토큰이 없습니다.") String resetToken,
        @JsonProperty("new_password") @NotBlank(message = "새 비밀번호를 입력해주세요.") String newPassword
) {
}
