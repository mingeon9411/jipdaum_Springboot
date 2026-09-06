package com.jipdaum_spring.dto.inquiry;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jipdaum_spring.domain.inquiry.Inquiry;

import java.time.LocalDateTime;

public record InquiryResponse(
        Long id,
        String title,
        String content,
        String answer,
        @JsonProperty("answered_at") LocalDateTime answeredAt,
        @JsonProperty("created_at") LocalDateTime createdAt
) {
    public static InquiryResponse from(Inquiry inquiry) {
        return new InquiryResponse(
                inquiry.getId(),
                inquiry.getTitle(),
                inquiry.getContent(),
                inquiry.getAnswer(),
                inquiry.getAnsweredAt(),
                inquiry.getCreatedAt()
        );
    }
}
