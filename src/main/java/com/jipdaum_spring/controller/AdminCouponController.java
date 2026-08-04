package com.jipdaum_spring.controller;

import com.jipdaum_spring.dto.coupon.CouponCreateRequest;
import com.jipdaum_spring.dto.coupon.CouponResponse;
import com.jipdaum_spring.service.AdminCouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final AdminCouponService adminCouponService;

    @PostMapping
    public ResponseEntity<CouponResponse> create(@Valid @RequestBody CouponCreateRequest request) {
        CouponResponse response = adminCouponService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{couponId}")
    public ResponseEntity<CouponResponse> update(
            @PathVariable Long couponId,
            @Valid @RequestBody CouponCreateRequest request
    ) {
        return ResponseEntity.ok(adminCouponService.update(couponId, request));
    }

    @DeleteMapping("/{couponId}")
    public ResponseEntity<Void> delete(@PathVariable Long couponId) {
        adminCouponService.delete(couponId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<CouponResponse>> getAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(adminCouponService.getAll(pageable));
    }
}
