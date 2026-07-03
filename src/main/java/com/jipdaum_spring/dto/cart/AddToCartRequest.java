package com.jipdaum_spring.dto.cart;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddToCartRequest(
        @NotNull(message = "상품을 선택해주세요.") Long product,
        @JsonAlias("product_option") Long option,
        @Min(value = 1, message = "수량은 1개 이상이어야 합니다.") Integer quantity
) {
    public int quantityOrDefault() {
        return quantity != null ? quantity : 1;
    }
}