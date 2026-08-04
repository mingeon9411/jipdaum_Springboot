package com.jipdaum_spring.dto.coupon;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

public record CouponCreateRequest(
        @NotBlank(message = "쿠폰 코드를 입력해주세요.") String code,
        @NotBlank(message = "쿠폰명을 입력해주세요.") String name,
        @NotBlank(message = "할인 방식을 선택해주세요.")
        @Pattern(regexp = "FIXED|PERCENT", message = "할인 방식은 FIXED 또는 PERCENT여야 합니다.") String discountType,
        @NotNull(message = "할인값을 입력해주세요.") @PositiveOrZero Integer discountValue,
        @PositiveOrZero Integer minOrderAmount,
        @PositiveOrZero Integer maxDiscountAmount,
        LocalDate expiryDate,
        @PositiveOrZero Integer usageLimit,
        Boolean isActive,
        Boolean isPersonal
) {
}
