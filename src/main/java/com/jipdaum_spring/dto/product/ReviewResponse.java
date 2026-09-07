package com.jipdaum_spring.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jipdaum_spring.domain.product.Review;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long product,
        @JsonProperty("product_name") String productName,
        @JsonProperty("product_thumbnail_url") String productThumbnailUrl,
        Long user,
        @JsonProperty("user_nickname") String userNickname,
        Integer rating,
        String title,
        String comment,
        @JsonProperty("review_image_url") String reviewImageUrl,
        @JsonProperty("created_at") LocalDateTime createdAt
) {
    public static ReviewResponse from(Review r) {
        return new ReviewResponse(
                r.getId(),
                r.getProduct().getId(),
                r.getProduct().getName(),
                r.getProduct().getThumbnailUrl(),
                r.getUser() != null ? r.getUser().getId() : null,
                r.getUser() != null ? r.getUser().getNickname() : "",
                r.getRating(),
                r.getTitle(),
                r.getComment(),
                r.getReviewImageUrl(),
                r.getCreatedAt()
        );
    }
}
