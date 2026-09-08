package com.jipdaum_spring.dto.auth;

import com.jipdaum_spring.dto.coupon.MyCouponResponse;

import java.util.List;

public record RegisterResponse(
        String nickname,
        String email,
        List<MyCouponResponse> coupons,
        String access,
        String refresh
) {
    public RegisterResponse(String nickname, String email, List<MyCouponResponse> coupons) {
        this(nickname, email, coupons, null, null);
    }
}
