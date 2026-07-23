package com.jipdaum_spring.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public record OrderTrackingResponse(
        @JsonProperty("order_id") Long orderId,
        String carrier,
        @JsonProperty("tracking_number") String trackingNumber,
        @JsonProperty("current_step") String currentStep,
        @JsonProperty("is_mock") boolean isMock,
        List<TrackingStep> steps
) {
    public record TrackingStep(
            String code,
            String label,
            LocalDateTime time,
            boolean done
    ) {}
}
