package com.jipdaum_spring.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jipdaum_spring.domain.product.Review;

import java.time.LocalDateTime;

public record CreateReviewResponse(
        Long id,
        Integer rating,
        String comment,
        @JsonProperty("user_nickname") String userNickname,
        @JsonProperty("created_at") LocalDateTime createdAt
) {
    public static CreateReviewResponse of(Review review, String userNickname) {
        return new CreateReviewResponse(
                review.getId(),
                review.getRating(),
                review.getComment(),
                userNickname,
                review.getCreatedAt()
        );
    }
}