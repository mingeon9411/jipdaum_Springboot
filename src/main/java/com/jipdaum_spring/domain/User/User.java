package com.jipdaum_spring.domain.User;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String name;
    private String profileImage;
    private String provider;
    private String providerId;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Builder
    public User(String email, String name, String profileImage, String provider, String providerId, Role role) {
        this.email = email;
        this.name = name;
        this.profileImage = profileImage;
        this.provider = provider;
        this.providerId = providerId;
        this.role = role;
    }

    public User update(String name, String profileImage) {
        this.name = name;
        this.profileImage = profileImage;
        return this;
    }

    public enum Role {
        USER, ADMIN
    }
}