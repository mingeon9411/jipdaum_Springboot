package com.jipdaum_spring.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateReviewRequest(
        @NotNull(message = "평점을 입력해주세요.") @Min(1) @Max(5) Integer rating,
        String comment,
        @JsonProperty("review_image_url") String reviewImageUrl
) {
}