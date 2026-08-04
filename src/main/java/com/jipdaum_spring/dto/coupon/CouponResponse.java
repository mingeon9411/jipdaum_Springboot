package com.jipdaum_spring.dto.coupon;

import com.jipdaum_spring.domain.coupon.Coupon;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CouponResponse(
        Long id,
        String code,
        String name,
        String discountType,
        Integer discountValue,
        Integer minOrderAmount,
        Integer maxDiscountAmount,
        LocalDate expiryDate,
        Integer usageLimit,
        Integer usedCount,
        Boolean isActive,
        Boolean isPersonal,
        LocalDateTime createdAt
) {
    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getCode(),
                coupon.getName(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMinOrderAmount(),
                coupon.getMaxDiscountAmount(),
                coupon.getExpiryDate(),
                coupon.getUsageLimit(),
                coupon.getUsedCount(),
                coupon.getIsActive(),
                coupon.getIsPersonal(),
                coupon.getCreatedAt()
        );
    }
}
