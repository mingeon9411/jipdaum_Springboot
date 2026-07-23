package com.jipdaum_spring.controller;

import com.jipdaum_spring.dto.order.CreateOrderRequest;
import com.jipdaum_spring.dto.order.PaymentReadyRequest;
import com.jipdaum_spring.dto.order.PaymentVerifyRequest;
import com.jipdaum_spring.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    public ResponseEntity<?> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(201).body(orderService.createOrder(request));
    }

    @PostMapping("/payment-ready")
    public ResponseEntity<?> paymentReady(@Valid @RequestBody PaymentReadyRequest request) {
        return ResponseEntity.ok(orderService.readyPayment(request));
    }

    @PostMapping("/payment-verify")
    public ResponseEntity<?> paymentVerify(@Valid @RequestBody PaymentVerifyRequest request) {
        return ResponseEntity.ok(orderService.verifyPayment(request));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrder(id));
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory() {
        return ResponseEntity.ok(orderService.getOrderHistory());
    }

    @GetMapping("/{id}/tracking")
    public ResponseEntity<?> getTracking(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getTracking(id));
    }
}