package com.jipdaum_spring.service.chat;

import com.jipdaum_spring.dto.product.ProductDetailResponse;

import java.util.List;
import java.util.Map;

/**
 * ProductSearchTool의 실행 결과. Gemini에게는 토큰 절약을 위해 trimmed된 forModel만 보내고,
 * products(전체 상품 정보)는 GeminiClient가 별도로 모아뒀다가 프론트가 화면에 상품 카드를
 * 렌더링할 수 있도록 채팅 응답에 그대로 실어 보낸다 — 챗봇이 실제로 찾은 상품과 화면에
 * 뜨는 상품이 항상 일치하도록(프론트가 별도 키워드 매칭으로 추측하지 않도록) 하기 위함.
 */
public record ProductListResult(List<Map<String, Object>> forModel, List<ProductDetailResponse> products) {
}
