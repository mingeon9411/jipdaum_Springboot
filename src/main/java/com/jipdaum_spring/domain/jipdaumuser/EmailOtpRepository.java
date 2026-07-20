package com.jipdaum_spring.domain.jipdaumuser;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {
    Optional<EmailOtp> findFirstByUserIdAndEmailAndCodeAndUsedFalseOrderByCreatedAtDesc(
            Long userId, String email, String code);

    void deleteAllByUserIdAndUsedFalse(Long userId);
}
