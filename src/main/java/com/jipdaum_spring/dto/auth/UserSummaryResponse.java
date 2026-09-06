package com.jipdaum_spring.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jipdaum_spring.domain.jipdaumuser.JipdaumUser;

public record UserSummaryResponse(
        Long id,
        String username,
        String nickname,
        String email,
        @JsonProperty("is_email_verified") boolean emailVerified,
        @JsonProperty("has_security_question") boolean hasSecurityQuestion
) {
    public static UserSummaryResponse from(JipdaumUser user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                Boolean.TRUE.equals(user.getEmailVerified()),
                user.getSecurityQuestion() != null
        );
    }
}
