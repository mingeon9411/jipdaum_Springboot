package com.jipdaum_spring.controller;

import com.jipdaum_spring.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    public ResponseEntity<?> validateCoupon(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(couponService.validateCoupon(body));
    }
}
