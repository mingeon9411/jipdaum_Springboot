package com.jipdaum_spring.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
        @JsonProperty("product_id") @NotNull(message = "상품을 선택해주세요.") Long productId,
        @JsonProperty("option_id") Long optionId,
        @Min(value = 1, message = "수량은 1개 이상이어야 합니다.") Integer quantity
) {
    public int quantityOrDefault() {
        return quantity != null ? quantity : 1;
    }
}