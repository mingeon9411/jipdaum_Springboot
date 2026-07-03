package com.jipdaum_spring.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaymentReadyResponse(
        @JsonProperty("merchant_uid") String merchantUid,
        Integer amount
) {
}