package com.jipdaum_spring.service.chat;

import com.jipdaum_spring.dto.product.ProductDetailResponse;
import com.jipdaum_spring.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 상품 검색은 로그인 여부와 무관하게 공개 정보라 별도 인증 스코핑 없이 그대로 노출한다
 * (기존 GET /api/shop/products/**도 permitAll인 것과 동일한 정책).
 */
@Component
@RequiredArgsConstructor
public class ProductSearchTool implements ChatTool {

    // 토큰/비용 방어: 한 번에 너무 많은 상품을 LLM 컨텍스트에 넘기지 않는다.
    private static final int MAX_RESULTS = 8;

    private final ProductService productService;

    @Override
    public String name() {
        return "search_products";
    }

    @Override
    public String description() {
        return "집다움 쇼핑몰의 상품을 이름/키워드로 검색한다. 검색어를 비우면 전체 상품 목록 상위 몇 개를 반환한다.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "search", Map.of("type", "STRING", "description", "검색할 상품명 또는 키워드 (선택, 비우면 전체 조회)"),
                        "categoryId", Map.of("type", "INTEGER", "description", "카테고리 ID로 필터링 (선택)")
                )
        );
    }

    @Override
    public Object execute(Map<String, Object> args) {
        String search = args.get("search") != null ? args.get("search").toString() : null;
        Long categoryId = ChatToolArgs.asLong(args.get("categoryId"));

        List<ProductDetailResponse> products = productService.getProducts(search, categoryId);
        return products.stream()
                .limit(MAX_RESULTS)
                .map(p -> Map.of(
                        "id", p.id(),
                        "name", p.name(),
                        "brand", p.brand() != null ? p.brand() : "",
                        "basePrice", p.basePrice(),
                        "categoryName", p.categoryName() != null ? p.categoryName() : ""
                ))
                .toList();
    }
}
