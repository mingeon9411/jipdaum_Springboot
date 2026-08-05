package com.jipdaum_spring.dto.user;

public record MeResponse(
        Long id,
        String email,
        String name,
        String nickname,
        String profileImage,
        String provider
) {
}
