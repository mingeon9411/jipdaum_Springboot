package com.jipdaum_spring.domain.payment.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VerifyRequest {

    private Long orderId;
    private String paymentId;
}