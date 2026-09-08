package com.jipdaum_spring.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jipdaum_spring.domain.product.Product;

import java.time.LocalDateTime;
import java.util.List;

public record ProductDetailResponse(
        Long id,
        String name,
        String brand,
        @JsonProperty("base_price") Integer basePrice,
        String description,
        @JsonProperty("thumbnail_url") String thumbnailUrl,
        Long category,
        @JsonProperty("category_name") String categoryName,
        @JsonProperty("same_day_shipping") boolean sameDayShipping,
        @JsonProperty("created_at") LocalDateTime createdAt,
        List<ProductOptionSummary> options
) {
    public static ProductDetailResponse from(Product p) {
        return new ProductDetailResponse(
                p.getId(),
                p.getName(),
                p.getBrand(),
                p.getBasePrice(),
                p.getDescription(),
                p.getThumbnailUrl(),
                p.getCategory() != null ? p.getCategory().getId() : null,
                p.getCategory() != null ? p.getCategory().getName() : null,
                p.isSameDayShipping(),
                p.getCreatedAt(),
                p.getOptions().stream().map(ProductOptionSummary::from).toList()
        );
    }
}
