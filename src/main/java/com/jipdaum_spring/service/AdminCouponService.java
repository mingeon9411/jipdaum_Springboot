package com.jipdaum_spring.service;

import com.jipdaum_spring.domain.coupon.Coupon;
import com.jipdaum_spring.domain.coupon.CouponRepository;
import com.jipdaum_spring.dto.coupon.CouponCreateRequest;
import com.jipdaum_spring.dto.coupon.CouponResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCouponService {

    private final CouponRepository couponRepository;

    @Transactional
    public CouponResponse create(CouponCreateRequest request) {
        Coupon coupon = new Coupon();
        applyRequest(coupon, request);
        coupon.setUsedCount(0);
        coupon.setCreatedAt(LocalDateTime.now());
        couponRepository.save(coupon);
        return CouponResponse.from(coupon);
    }

    @Transactional
    public CouponResponse update(Long couponId, CouponCreateRequest request) {
        Coupon coupon = findCoupon(couponId);
        applyRequest(coupon, request);
        return CouponResponse.from(coupon);
    }

    @Transactional
    public void delete(Long couponId) {
        couponRepository.delete(findCoupon(couponId));
    }

    public Page<CouponResponse> getAll(Pageable pageable) {
        return couponRepository.findAll(pageable).map(CouponResponse::from);
    }

    private void applyRequest(Coupon coupon, CouponCreateRequest request) {
        coupon.setCode(request.code().strip().toUpperCase());
        coupon.setName(request.name());
        coupon.setDiscountType(request.discountType());
        coupon.setDiscountValue(request.discountValue());
        coupon.setMinOrderAmount(request.minOrderAmount() != null ? request.minOrderAmount() : 0);
        coupon.setMaxDiscountAmount(request.maxDiscountAmount());
        coupon.setExpiryDate(request.expiryDate());
        coupon.setUsageLimit(request.usageLimit());
        coupon.setIsActive(request.isActive() != null ? request.isActive() : true);
        coupon.setIsPersonal(request.isPersonal() != null ? request.isPersonal() : false);
    }

    private Coupon findCoupon(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 쿠폰입니다."));
    }
}
