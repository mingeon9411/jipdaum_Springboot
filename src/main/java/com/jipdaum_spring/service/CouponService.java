package com.jipdaum_spring.service;

import com.jipdaum_spring.domain.coupon.Coupon;
import com.jipdaum_spring.domain.coupon.CouponRepository;
import com.jipdaum_spring.domain.coupon.UserCoupon;
import com.jipdaum_spring.domain.coupon.UserCouponRepository;
import com.jipdaum_spring.domain.user.JipdaumUser;
import com.jipdaum_spring.domain.user.JipdaumUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final UserCouponRepository userCouponRepository;
    private final CouponRepository couponRepository;
    private final JipdaumUserRepository jipdaumUserRepository;

    private JipdaumUser getCurrentUser() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return jipdaumUserRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    public List<Map<String, Object>> getMyCoupons() {
        JipdaumUser user = getCurrentUser();
        return userCouponRepository.findByUserAndIsUsedFalse(user).stream().map(uc -> {
            Coupon c = uc.getCoupon();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", uc.getId());
            m.put("code", c.getCode());
            m.put("name", c.getName());
            m.put("discount_type", c.getDiscountType());
            m.put("discount_value", c.getDiscountValue());
            m.put("min_order_amount", c.getMinOrderAmount());
            m.put("max_discount_amount", c.getMaxDiscountAmount());
            m.put("expiry_date", c.getExpiryDate() != null
                    ? c.getExpiryDate().format(DateTimeFormatter.ISO_DATE)
                    : null);
            m.put("is_used", uc.getIsUsed());
            return (Map<String, Object>) m;
        }).toList();
    }

    public Map<String, Object> validateCoupon(Map<String, Object> body) {
        JipdaumUser user = getCurrentUser();
        String code = body.get("code").toString().strip().toUpperCase();
        int orderAmount = Integer.parseInt(body.get("order_amount").toString());

        Optional<UserCoupon> ucOpt = userCouponRepository.findByUserAndCoupon_CodeAndIsUsedFalse(user, code);
        Coupon coupon = ucOpt.map(UserCoupon::getCoupon)
                .orElseGet(() -> couponRepository.findByCodeAndIsPersonalFalse(code).orElse(null));

        if (coupon == null)
            return Map.of("valid", false, "message", "유효하지 않은 쿠폰입니다.");
        if (!coupon.isValid())
            return Map.of("valid", false, "message", "사용할 수 없는 쿠폰입니다.");

        return Map.of(
                "valid", true,
                "discount_amount", coupon.calcDiscount(orderAmount),
                "coupon_name", coupon.getName()
        );
    }
}
