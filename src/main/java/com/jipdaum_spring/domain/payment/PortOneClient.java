package com.jipdaum_spring.domain.payment;

import com.jipdaum_spring.domain.payment.dto.PaymentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class PortOneClient {

    private final WebClient webClient = WebClient.create("https://api.portone.io");

    @Value("${portone.api-secret}")
    private String apiSecret;

    public PaymentResponse getPayment(String paymentId) {
        return webClient.get()
                .uri("/payments/{paymentId}", paymentId)
                .header("Authorization", "PortOne " + apiSecret)
                .retrieve()
                .bodyToMono(PaymentResponse.class)
                .block();
    }
}