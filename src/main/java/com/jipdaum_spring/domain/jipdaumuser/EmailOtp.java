package com.jipdaum_spring.domain.jipdaumuser;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "JIPDAUM_EMAIL_OTP")
@Getter
@Setter
@NoArgsConstructor
public class EmailOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "email")
    private String email;

    @Column(name = "code")
    private String code;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "is_used")
    private Boolean used;

    public EmailOtp(Long userId, String email, String code) {
        this.userId = userId;
        this.email = email;
        this.code = code;
        this.createdAt = LocalDateTime.now();
        this.used = false;
    }
}
