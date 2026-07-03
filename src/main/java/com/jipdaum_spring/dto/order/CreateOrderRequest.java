package com.jipdaum_spring.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateOrderRequest(
        @JsonProperty("shipping_addr") @NotBlank(message = "배송지를 입력해주세요.") String shippingAddr,
        @JsonProperty("coupon_code") String couponCode,
        @NotEmpty(message = "주문 상품이 없습니다.") @Valid List<OrderItemRequest> items
) {
}