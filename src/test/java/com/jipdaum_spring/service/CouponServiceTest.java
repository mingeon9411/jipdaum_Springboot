package com.jipdaum_spring.service;

import com.jipdaum_spring.domain.coupon.Coupon;
import com.jipdaum_spring.domain.coupon.CouponRepository;
import com.jipdaum_spring.domain.coupon.UserCouponRepository;
import com.jipdaum_spring.domain.jipdaumuser.JipdaumUser;
import com.jipdaum_spring.dto.coupon.ValidateCouponRequest;
import com.jipdaum_spring.dto.coupon.ValidateCouponResponse;
import com.jipdaum_spring.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock private UserCouponRepository userCouponRepository;
    @Mock private CouponRepository couponRepository;
    @Mock private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private CouponService couponService;

    private JipdaumUser user;

    @BeforeEach
    void setUp() {
        user = new JipdaumUser();
        ReflectionTestUtils.setField(user, "id", 1L);
        lenient().when(currentUserProvider.getCurrentUser()).thenReturn(user);
    }

    @Test
    void 존재하지_않는_쿠폰코드는_invalid를_반환한다() {
        when(userCouponRepository.findByUserAndCoupon_CodeAndIsUsedFalse(user, "NOPE"))
                .thenReturn(Optional.empty());
        when(couponRepository.findByCodeAndIsPersonalFalse("NOPE")).thenReturn(Optional.empty());

        ValidateCouponResponse response = couponService.validateCoupon(
                new ValidateCouponRequest("nope", 10000));

        assertThat(response.valid()).isFalse();
        assertThat(response.message()).isEqualTo("유효하지 않은 쿠폰입니다.");
    }

    @Test
    void 만료된_쿠폰은_invalid를_반환한다() {
        Coupon coupon = new Coupon();
        coupon.setDiscountType("FIXED");
        coupon.setDiscountValue(1000);
        coupon.setIsActive(false);

        when(userCouponRepository.findByUserAndCoupon_CodeAndIsUsedFalse(user, "OLD"))
                .thenReturn(Optional.empty());
        when(couponRepository.findByCodeAndIsPersonalFalse("OLD")).thenReturn(Optional.of(coupon));

        ValidateCouponResponse response = couponService.validateCoupon(
                new ValidateCouponRequest("old", 10000));

        assertThat(response.valid()).isFalse();
        assertThat(response.message()).isEqualTo("사용할 수 없는 쿠폰입니다.");
    }

    @Test
    void 유효한_쿠폰은_할인금액과_함께_valid를_반환한다() {
        Coupon coupon = new Coupon();
        coupon.setName("웰컴 쿠폰");
        coupon.setDiscountType("PERCENT");
        coupon.setDiscountValue(10);
        coupon.setIsActive(true);

        when(userCouponRepository.findByUserAndCoupon_CodeAndIsUsedFalse(user, "WELCOME"))
                .thenReturn(Optional.empty());
        when(couponRepository.findByCodeAndIsPersonalFalse("WELCOME")).thenReturn(Optional.of(coupon));

        ValidateCouponResponse response = couponService.validateCoupon(
                new ValidateCouponRequest("welcome", 20000));

        assertThat(response.valid()).isTrue();
        assertThat(response.discountAmount()).isEqualTo(2000);
        assertThat(response.couponName()).isEqualTo("웰컴 쿠폰");
    }
}
