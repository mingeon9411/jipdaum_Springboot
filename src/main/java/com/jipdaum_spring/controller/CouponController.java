package com.jipdaum_spring.controller;

import com.jipdaum_spring.dto.coupon.ValidateCouponRequest;
import com.jipdaum_spring.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/my")
    public ResponseEntity<?> getMyCoupons() {
        return ResponseEntity.ok(couponService.getMyCoupons());
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateCoupon(@Valid @RequestBody ValidateCouponRequest request) {
        return ResponseEntity.ok(couponService.validateCoupon(request));
    }
}