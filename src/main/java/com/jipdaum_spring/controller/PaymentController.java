package com.jipdaum_spring.controller;


import com.jipdaum_spring.domain.payment.PaymentVerificationException;
import com.jipdaum_spring.domain.payment.PaymentVerificationService;
import com.jipdaum_spring.domain.payment.dto.VerifyRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentVerificationService paymentVerificationService;

    @PostMapping("/verify")
    public ResponseEntity<Void> verify(@RequestBody VerifyRequest request) {
        paymentVerificationService.verifyAndComplete(request.getOrderId(), request.getPaymentId());
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(PaymentVerificationException.class)
    public ResponseEntity<String> handlePaymentVerification(PaymentVerificationException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<String> handleWebClientResponse(WebClientResponseException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body("PortOne 결제 조회 실패: " + e.getStatusCode());
    }
}
