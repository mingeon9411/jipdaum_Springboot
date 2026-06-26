package com.jipdaum_spring.domain.payment;

import com.jipdaum_spring.domain.order.Order;
import com.jipdaum_spring.domain.payment.dto.PaymentResponse;
import com.jipdaum_spring.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor

public class PaymentVerificationService {

    private final PortOneClient portOneClient;
    private final OrderRepository orderRepository;

    @Transactional
    public void verifyAndComplete(Long orderId, String paymentId) {
        PaymentResponse verified = portOneClient.getPayment(paymentId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        if (order.getTotalPrice().compareTo(verified.getAmount().getTotal()) != 0) {
            throw new PaymentVerificationException("결제 금액이 일치하지 않습니다.");

        }
        order.markAsPaid();
    }
}