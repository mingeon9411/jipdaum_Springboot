package com.jipdaum_spring.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "아이디 또는 이메일을 입력해주세요.") String username,
        @NotBlank(message = "비밀번호를 입력해주세요.") String password,
        @JsonProperty("recaptcha_token") String recaptchaToken
) {
}
