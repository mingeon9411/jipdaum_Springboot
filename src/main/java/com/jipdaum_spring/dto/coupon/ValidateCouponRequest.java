package com.jipdaum_spring.dto.coupon;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ValidateCouponRequest(
        @NotBlank(message = "쿠폰 코드를 입력해주세요.") String code,
        @JsonProperty("order_amount") @NotNull(message = "주문 금액이 필요합니다.") @Min(0) Integer orderAmount
) {
}