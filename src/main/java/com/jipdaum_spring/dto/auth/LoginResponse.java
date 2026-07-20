package com.jipdaum_spring.dto.auth;

public record LoginResponse(String access, String refresh, UserSummaryResponse user) {
}
