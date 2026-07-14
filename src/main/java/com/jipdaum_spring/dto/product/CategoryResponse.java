package com.jipdaum_spring.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jipdaum_spring.domain.product.Category;

public record CategoryResponse(
        Long id,
        String name,
        Long parent,
        @JsonProperty("parent_name") String parentName
) {
    public static CategoryResponse from(Category category) {
        Category parent = category.getParent();
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                parent != null ? parent.getId() : null,
                parent != null ? parent.getName() : null
        );
    }
}
