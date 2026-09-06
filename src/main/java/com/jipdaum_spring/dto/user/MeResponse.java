package com.jipdaum_spring.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MeResponse(
        Long id,
        String email,
        String name,
        String nickname,
        String profileImage,
        String provider,
        @JsonProperty("has_security_question") boolean hasSecurityQuestion,
        @JsonProperty("security_question") String securityQuestion
) {
}
