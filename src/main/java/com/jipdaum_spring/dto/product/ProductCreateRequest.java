package com.jipdaum_spring.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProductCreateRequest(
        @NotBlank(message = "상품명을 입력해주세요.") String name,
        String brand,
        @NotNull(message = "가격을 입력해주세요.") @Positive(message = "가격은 0보다 커야 합니다.") Integer basePrice,
        String description,
        String thumbnailUrl,
        @NotNull(message = "카테고리를 선택해주세요.") Long categoryId
) {
}