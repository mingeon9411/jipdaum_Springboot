package com.jipdaum_spring.service;

import com.jipdaum_spring.domain.coupon.Coupon;
import com.jipdaum_spring.domain.coupon.CouponRepository;
import com.jipdaum_spring.domain.coupon.UserCoupon;
import com.jipdaum_spring.domain.coupon.UserCouponRepository;
import com.jipdaum_spring.domain.jipdaumuser.JipdaumUser;
import com.jipdaum_spring.dto.coupon.MyCouponResponse;
import com.jipdaum_spring.dto.coupon.ValidateCouponRequest;
import com.jipdaum_spring.dto.coupon.ValidateCouponResponse;
import com.jipdaum_spring.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final UserCouponRepository userCouponRepository;
    private final CouponRepository couponRepository;
    private final CurrentUserProvider currentUserProvider;

    private JipdaumUser getCurrentUser() {
        return currentUserProvider.getCurrentUser();
    }

    public List<MyCouponResponse> getMyCoupons() {
        JipdaumUser user = getCurrentUser();
        return userCouponRepository.findByUserAndIsUsedFalse(user).stream()
                .map(MyCouponResponse::from)
                .toList();
    }

    public ValidateCouponResponse validateCoupon(ValidateCouponRequest request) {
        JipdaumUser user = getCurrentUser();
        String code = request.code().strip().toUpperCase();

        Optional<UserCoupon> ucOpt = userCouponRepository.findByUserAndCoupon_CodeAndIsUsedFalse(user, code);
        Coupon coupon = ucOpt.map(UserCoupon::getCoupon)
                .orElseGet(() -> couponRepository.findByCodeAndIsPersonalFalse(code).orElse(null));

        if (coupon == null)
            return ValidateCouponResponse.invalid("유효하지 않은 쿠폰입니다.");
        if (!coupon.isValid())
            return ValidateCouponResponse.invalid("사용할 수 없는 쿠폰입니다.");

        return ValidateCouponResponse.valid(coupon.calcDiscount(request.orderAmount()), coupon.getName());
    }
}