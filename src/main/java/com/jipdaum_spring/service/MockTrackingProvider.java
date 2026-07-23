package com.jipdaum_spring.service;

import com.jipdaum_spring.domain.order.Order;
import com.jipdaum_spring.dto.order.OrderTrackingResponse;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 실제 택배사 API 없이, 결제 완료 시각으로부터 얼마나 지났는지를 기준으로
 * 배송 단계를 그럴듯하게 계산해서 보여주는 mock provider.
 * 24시간 단위로 한 단계씩 진행한다 (상품준비중 -> 집화완료 -> 배송중 -> 배송완료).
 */
@Service
public class MockTrackingProvider implements TrackingProvider {

    private static final int HOURS_PER_STEP = 24;
    private static final String[] CODES = {"PREPARING", "PICKED_UP", "IN_TRANSIT", "DELIVERED"};
    private static final String[] LABELS = {"상품 준비중", "집화 완료", "배송중", "배송 완료"};

    @Override
    public OrderTrackingResponse getTracking(Order order) {
        LocalDateTime base = (order.getPayment() != null && order.getPayment().getPaidAt() != null)
                ? order.getPayment().getPaidAt()
                : order.getOrderDate();

        long elapsedHours = Math.max(0, Duration.between(base, LocalDateTime.now()).toHours());
        int currentIndex = (int) Math.min(CODES.length - 1, elapsedHours / HOURS_PER_STEP);

        List<OrderTrackingResponse.TrackingStep> steps = new ArrayList<>();
        for (int i = 0; i < CODES.length; i++) {
            boolean done = i <= currentIndex;
            LocalDateTime time = done ? base.plusHours((long) i * HOURS_PER_STEP) : null;
            steps.add(new OrderTrackingResponse.TrackingStep(CODES[i], LABELS[i], time, done));
        }

        return new OrderTrackingResponse(
                order.getId(),
                order.getCarrier(),
                order.getTrackingNumber(),
                CODES[currentIndex],
                true,
                steps
        );
    }
}
