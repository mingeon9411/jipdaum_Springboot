package com.jipdaum_spring.controller;

import com.jipdaum_spring.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/shop/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(201).body(orderService.createOrder(body));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<?> processPayment(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(orderService.processPayment(id, body));
    }

    @PostMapping("/payment-ready")
    public ResponseEntity<?> paymentReady(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(orderService.readyPayment(body));
    }

    @PostMapping("/payment-verify")
    public ResponseEntity<?> paymentVerify(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(orderService.verifyPayment(body));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrder(id));
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory() {
        return ResponseEntity.ok(orderService.getOrderHistory());
    }
}
