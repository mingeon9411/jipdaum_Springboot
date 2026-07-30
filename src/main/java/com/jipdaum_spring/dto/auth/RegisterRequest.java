package com.jipdaum_spring.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank(message = "닉네임을 입력해주세요.") String nickname,
        @NotBlank(message = "이메일을 입력해주세요.") @Email(message = "올바른 이메일 형식이 아닙니다.") String email,
        @NotBlank(message = "비밀번호를 입력해주세요.") String password,
        @JsonProperty("password_confirm") @NotBlank(message = "비밀번호 확인을 입력해주세요.") String passwordConfirm,
        @JsonProperty("recaptcha_token") String recaptchaToken
) {
}
