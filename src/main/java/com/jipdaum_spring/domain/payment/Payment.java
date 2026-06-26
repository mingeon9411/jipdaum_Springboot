package com.jipdaum_spring.domain.payment;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String paymentId;

    private BigDecimal amount;

    private String status;

    private Long orderId;

    public Payment(String paymentId, BigDecimal amount, String status, Long orderId) {
        this.paymentId = paymentId;
        this.amount = amount;
        this.status = status;
        this.orderId = orderId;
    }
}