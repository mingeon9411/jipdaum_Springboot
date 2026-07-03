package com.jipdaum_spring.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentReadyRequest(
        @JsonProperty("order_id") @NotNull(message = "주문을 선택해주세요.") Long orderId,
        @NotBlank(message = "결제 수단을 선택해주세요.") String method
) {
}