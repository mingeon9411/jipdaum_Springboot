package com.jipdaum_spring.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SocialCaptchaRequest(
        @JsonProperty("recaptcha_token") String recaptchaToken
) {
}
