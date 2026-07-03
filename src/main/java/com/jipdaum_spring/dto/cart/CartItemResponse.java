package com.jipdaum_spring.dto.cart;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jipdaum_spring.domain.order.Cart;

public record CartItemResponse(
        Long id,
        @JsonProperty("product_id") Long productId,
        @JsonProperty("product_name") String productName,
        Integer price,
        String image,
        @JsonProperty("option_id") Long optionId,
        @JsonProperty("option_name") String optionName,
        Integer quantity
) {
    public static CartItemResponse from(Cart item) {
        return new CartItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getBasePrice(),
                item.getProduct().getThumbnailUrl(),
                item.getOption() != null ? item.getOption().getId() : null,
                item.getOption() != null
                        ? item.getOption().getOptionName() + ": " + item.getOption().getOptionValue()
                        : null,
                item.getQuantity()
        );
    }
}