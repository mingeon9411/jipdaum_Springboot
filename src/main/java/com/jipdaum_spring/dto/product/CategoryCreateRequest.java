package com.jipdaum_spring.dto.product;

import jakarta.validation.constraints.NotBlank;

public record CategoryCreateRequest(
        @NotBlank(message = "카테고리명을 입력해주세요.") String name,
        Long parentId
) {
}
