package com.jipdaum_spring.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CancelOrderResponse(
        String message,
        @JsonProperty("order_id") Long orderId
) {
}