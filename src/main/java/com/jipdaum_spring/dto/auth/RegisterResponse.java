package com.jipdaum_spring.dto.auth;

import com.jipdaum_spring.dto.coupon.MyCouponResponse;

import java.util.List;

public record RegisterResponse(String nickname, String email, List<MyCouponResponse> coupons) {
}
