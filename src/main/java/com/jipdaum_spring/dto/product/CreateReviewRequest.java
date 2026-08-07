package com.jipdaum_spring.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReviewRequest(
        @NotNull(message = "평점을 입력해주세요.") @Min(1) @Max(5) Integer rating,
        @Size(max = 100, message = "제목은 100자 이하여야 합니다.") String title,
        String comment,
        @JsonProperty("review_image_url") String reviewImageUrl
) {
}