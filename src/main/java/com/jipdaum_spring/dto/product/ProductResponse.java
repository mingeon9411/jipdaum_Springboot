package com.jipdaum_spring.dto.product;

import com.jipdaum_spring.domain.product.Product;

import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        String brand,
        Integer basePrice,
        String description,
        String thumbnailUrl,
        Long categoryId,
        String categoryName,
        LocalDateTime createdAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getBrand(),
                product.getBasePrice(),
                product.getDescription(),
                product.getThumbnailUrl(),
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getCreatedAt()
        );
    }
}