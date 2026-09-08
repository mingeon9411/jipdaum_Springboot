package com.jipdaum_spring.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IdentityVerificationVerifyRequest(
        @JsonProperty("identity_verification_id")
        @NotBlank(message = "본인인증 식별자가 필요합니다.")
        @Size(max = 128, message = "본인인증 식별자가 너무 깁니다.")
        String identityVerificationId
) {
}
