package com.jipdaum_spring.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jipdaum_spring.domain.product.Review;

import java.time.LocalDateTime;

public record CreateReviewResponse(
        Long id,
        Integer rating,
        String title,
        String comment,
        @JsonProperty("user_nickname") String userNickname,
        @JsonProperty("review_image_url") String reviewImageUrl,
        @JsonProperty("created_at") LocalDateTime createdAt
) {
    public static CreateReviewResponse of(Review review, String userNickname) {
        return new CreateReviewResponse(
                review.getId(),
                review.getRating(),
                review.getTitle(),
                review.getComment(),
                userNickname,
                review.getReviewImageUrl(),
                review.getCreatedAt()
        );
    }
}