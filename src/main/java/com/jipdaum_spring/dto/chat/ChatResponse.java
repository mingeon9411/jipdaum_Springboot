package com.jipdaum_spring.dto.chat;

import com.jipdaum_spring.dto.product.ProductDetailResponse;

import java.util.List;

/**
 * products는 이번 대화에서 search_products 도구가 실제로 찾아낸 상품 전체 목록이다(없으면
 * 빈 리스트) — 프론트가 이 값을 그대로 화면 오른쪽 상품 패널에 렌더링한다. 규칙 기반 폴백
 * 답변에는 도구 호출이 없으므로 항상 빈 리스트가 채워진다.
 */
public record ChatResponse(String reply, List<ProductDetailResponse> products) {
    public ChatResponse(String reply) {
        this(reply, List.of());
    }
}
