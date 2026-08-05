package com.jipdaum_spring.dto.user;

import com.jipdaum_spring.domain.springuser.User;

public record UserResponse(
        Long id,
        String email,
        String name,
        String profileImage,
        String provider,
        String role
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail() != null ? user.getEmail() : "",
                user.getName() != null ? user.getName() : "",
                user.getProfileImage() != null ? user.getProfileImage() : "",
                user.getProvider() != null ? user.getProvider() : "",
                user.getRole().name()
        );
    }
}
