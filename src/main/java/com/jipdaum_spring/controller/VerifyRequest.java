package com.jipdaum_spring.controller;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VerifyRequest {
    private Long orderId;
    private String paymentId;
}

