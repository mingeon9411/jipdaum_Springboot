package com.jipdaum_spring.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateOrderResponse(
        @JsonProperty("order_id") Long orderId,
        @JsonProperty("total_amount") Integer totalAmount,
        @JsonProperty("payment_required") boolean paymentRequired
) {
}
