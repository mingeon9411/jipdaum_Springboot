package com.jipdaum_spring.domain.payment.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Getter
@NoArgsConstructor

public class PaymentResponse {

    private String id;
    private String status;
    private Amount amount;

    @Getter
    @NoArgsConstructor
    public static class Amount {
        private BigDecimal total;
    }
}