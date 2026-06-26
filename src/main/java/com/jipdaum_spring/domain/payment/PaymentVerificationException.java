package com.jipdaum_spring.domain.payment;

public class PaymentVerificationException extends RuntimeException {
    public PaymentVerificationException(String message) {
        super(message);
    }
}