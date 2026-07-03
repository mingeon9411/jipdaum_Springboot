package com.jipdaum_spring.dto.coupon;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jipdaum_spring.domain.coupon.Coupon;
import com.jipdaum_spring.domain.coupon.UserCoupon;

import java.time.format.DateTimeFormatter;

public record MyCouponResponse(
        Long id,
        String code,
        String name,
        @JsonProperty("discount_type") String discountType,
        @JsonProperty("discount_value") Integer discountValue,
        @JsonProperty("min_order_amount") Integer minOrderAmount,
        @JsonProperty("max_discount_amount") Integer maxDiscountAmount,
        @JsonProperty("expiry_date") String expiryDate,
        @JsonProperty("is_used") Boolean isUsed
) {
    public static MyCouponResponse from(UserCoupon uc) {
        Coupon c = uc.getCoupon();
        return new MyCouponResponse(
                uc.getId(),
                c.getCode(),
                c.getName(),
                c.getDiscountType(),
                c.getDiscountValue(),
                c.getMinOrderAmount(),
                c.getMaxDiscountAmount(),
                c.getExpiryDate() != null ? c.getExpiryDate().format(DateTimeFormatter.ISO_DATE) : null,
                uc.getIsUsed()
        );
    }
}