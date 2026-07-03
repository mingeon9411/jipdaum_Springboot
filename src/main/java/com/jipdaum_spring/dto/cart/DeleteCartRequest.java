package com.jipdaum_spring.dto.cart;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record DeleteCartRequest(
        @JsonProperty("item_id") @NotNull(message = "item_id가 필요합니다.") Long itemId
) {
}