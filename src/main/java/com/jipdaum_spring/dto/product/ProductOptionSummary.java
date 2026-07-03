package com.jipdaum_spring.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jipdaum_spring.domain.product.ProductOption;

public record ProductOptionSummary(
        Long id,
        @JsonProperty("option_name") String optionName,
        @JsonProperty("option_value") String optionValue,
        @JsonProperty("extra_price") Integer extraPrice,
        @JsonProperty("stock_count") Integer stockCount
) {
    public static ProductOptionSummary from(ProductOption option) {
        return new ProductOptionSummary(
                option.getId(),
                option.getOptionName(),
                option.getOptionValue(),
                option.getExtraPrice(),
                option.getStockCount()
        );
    }
}