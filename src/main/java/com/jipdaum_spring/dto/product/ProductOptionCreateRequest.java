package com.jipdaum_spring.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record ProductOptionCreateRequest(
        @NotBlank(message = "옵션명을 입력해주세요.") String optionName,
        @NotBlank(message = "옵션값을 입력해주세요.") String optionValue,
        Integer extraPrice,
        @PositiveOrZero(message = "재고는 0 이상이어야 합니다.") Integer stockCount
) {
}
