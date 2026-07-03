package com.jipdaum_spring.dto.coupon;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ValidateCouponResponse(
        boolean valid,
        String message,
        @JsonProperty("discount_amount") Integer discountAmount,
        @JsonProperty("coupon_name") String couponName
) {
    public static ValidateCouponResponse invalid(String message) {
        return new ValidateCouponResponse(false, message, null, null);
    }

    public static ValidateCouponResponse valid(int discountAmount, String couponName) {
        return new ValidateCouponResponse(true, null, discountAmount, couponName);
    }
}