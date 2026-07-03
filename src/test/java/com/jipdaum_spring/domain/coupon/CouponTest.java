package com.jipdaum_spring.domain.coupon;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CouponTest {

    private Coupon percentCoupon(int discountValue, Integer maxDiscount, Integer minOrderAmount) {
        Coupon coupon = new Coupon();
        coupon.setDiscountType("PERCENT");
        coupon.setDiscountValue(discountValue);
        coupon.setMaxDiscountAmount(maxDiscount);
        coupon.setMinOrderAmount(minOrderAmount);
        coupon.setIsActive(true);
        return coupon;
    }

    private Coupon fixedCoupon(int discountValue, Integer minOrderAmount) {
        Coupon coupon = new Coupon();
        coupon.setDiscountType("FIXED");
        coupon.setDiscountValue(discountValue);
        coupon.setMinOrderAmount(minOrderAmount);
        coupon.setIsActive(true);
        return coupon;
    }

    @Test
    void 퍼센트_할인은_주문금액에_비례하고_최대할인을_넘지_않는다() {
        Coupon coupon = percentCoupon(10, 5000, null);

        assertThat(coupon.calcDiscount(30000)).isEqualTo(3000);
        assertThat(coupon.calcDiscount(100000)).isEqualTo(5000); // maxDiscountAmount에 걸림
    }

    @Test
    void 정액_할인은_주문금액을_넘지_않는다() {
        Coupon coupon = fixedCoupon(5000, null);

        assertThat(coupon.calcDiscount(10000)).isEqualTo(5000);
        assertThat(coupon.calcDiscount(3000)).isEqualTo(3000); // 주문금액보다 클 수 없음
    }

    @Test
    void 최소주문금액_미달이면_할인이_0이다() {
        Coupon coupon = fixedCoupon(5000, 20000);

        assertThat(coupon.calcDiscount(10000)).isEqualTo(0);
    }

    @Test
    void 비활성_쿠폰은_유효하지_않다() {
        Coupon coupon = fixedCoupon(1000, null);
        coupon.setIsActive(false);

        assertThat(coupon.isValid()).isFalse();
    }

    @Test
    void 만료일이_지난_쿠폰은_유효하지_않다() {
        Coupon coupon = fixedCoupon(1000, null);
        coupon.setExpiryDate(LocalDate.now().minusDays(1));

        assertThat(coupon.isValid()).isFalse();
    }

    @Test
    void 사용한도를_채운_쿠폰은_유효하지_않다() {
        Coupon coupon = fixedCoupon(1000, null);
        coupon.setUsageLimit(10);
        coupon.setUsedCount(10);

        assertThat(coupon.isValid()).isFalse();
    }

    @Test
    void 사용한도가_남은_쿠폰은_유효하다() {
        Coupon coupon = fixedCoupon(1000, null);
        coupon.setUsageLimit(10);
        coupon.setUsedCount(9);

        assertThat(coupon.isValid()).isTrue();
    }
}
