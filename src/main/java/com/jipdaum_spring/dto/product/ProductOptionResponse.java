package com.jipdaum_spring.dto.product;

import com.jipdaum_spring.domain.product.ProductOption;

public record ProductOptionResponse(
        Long id,
        Long productId,
        String optionName,
        String optionValue,
        Integer extraPrice,
        Integer stockCount
) {
    public static ProductOptionResponse from(ProductOption option) {
        return new ProductOptionResponse(
                option.getId(),
                option.getProduct().getId(),
                option.getOptionName(),
                option.getOptionValue(),
                option.getExtraPrice(),
                option.getStockCount()
        );
    }
}
