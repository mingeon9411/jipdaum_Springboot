package com.jipdaum_spring.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jipdaum_spring.domain.order.Order;
import com.jipdaum_spring.domain.order.Payment;

import java.time.LocalDateTime;
import java.util.List;

public record OrderHistoryResponse(
        Long id,
        @JsonProperty("order_date") LocalDateTime orderDate,
        String status,
        @JsonProperty("status_display") String statusDisplay,
        @JsonProperty("total_amount") Integer totalAmount,
        @JsonProperty("shipping_addr") String shippingAddr,
        @JsonProperty("payment_method") String paymentMethod,
        @JsonProperty("payment_status") String paymentStatus,
        @JsonProperty("paid_at") LocalDateTime paidAt,
        List<OrderHistoryItemResponse> items
) {
    public static OrderHistoryResponse from(Order order) {
        Payment payment = order.getPayment();
        return new OrderHistoryResponse(
                order.getId(),
                order.getOrderDate(),
                order.getStatus(),
                order.getStatusDisplay(),
                order.getTotalAmount(),
                order.getShippingAddr(),
                payment != null ? payment.getMethodDisplay() : null,
                payment != null ? payment.getStatus() : null,
                payment != null ? payment.getPaidAt() : null,
                order.getItems().stream().map(OrderHistoryItemResponse::from).toList()
        );
    }
}