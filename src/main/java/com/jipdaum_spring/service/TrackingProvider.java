package com.jipdaum_spring.service;

import com.jipdaum_spring.domain.order.Order;
import com.jipdaum_spring.dto.order.OrderTrackingResponse;

/**
 * 운송장 조회 결과를 만들어주는 provider.
 * 지금은 MockTrackingProvider만 있지만, 실제 택배사 API 키가 생기면
 * 이 인터페이스를 구현하는 새 provider(e.g. SmartTrackingProvider)로
 * 교체하기만 하면 컨트롤러/서비스 쪽은 손댈 필요가 없다.
 */
public interface TrackingProvider {
    OrderTrackingResponse getTracking(Order order);
}
