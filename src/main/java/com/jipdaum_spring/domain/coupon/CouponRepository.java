package com.jipdaum_spring.domain.coupon;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCodeAndIsPersonalFalse(String code);
    Optional<Coupon> findByCode(String code);
}
