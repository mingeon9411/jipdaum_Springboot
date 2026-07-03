package com.jipdaum_spring.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jipdaum_spring.domain.order.OrderItem;

public record OrderHistoryItemResponse(
        @JsonProperty("product_name") String productName,
        @JsonProperty("product_image") String productImage,
        Integer quantity,
        @JsonProperty("ordered_price") Integer orderedPrice
) {
    public static OrderHistoryItemResponse from(OrderItem item) {
        return new OrderHistoryItemResponse(
                item.getProduct().getName(),
                item.getProduct().getThumbnailUrl(),
                item.getQuantity(),
                item.getOrderedPrice()
        );
    }
}