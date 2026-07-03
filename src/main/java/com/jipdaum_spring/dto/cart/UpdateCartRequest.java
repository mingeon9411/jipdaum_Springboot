package com.jipdaum_spring.dto.cart;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCartRequest(
        @JsonProperty("item_id") @NotNull(message = "item_id가 필요합니다.") Long itemId,
        @NotNull(message = "수량을 입력해주세요.") @Min(value = 1, message = "수량은 1개 이상이어야 합니다.") Integer quantity
) {
}