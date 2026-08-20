package com.jipdaum_spring.service.chat;

import com.jipdaum_spring.dto.product.ProductDetailResponse;
import com.jipdaum_spring.service.GeminiEmbeddingClient;
import com.jipdaum_spring.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 상품 검색은 로그인 여부와 무관하게 공개 정보라 별도 인증 스코핑 없이 그대로 노출한다
 * (기존 GET /api/shop/products/**도 permitAll인 것과 동일한 정책).
 *
 * search 파라미터가 있으면 우선 상품 임베딩 인덱스(ProductEmbeddingIndex)로 의미 기반 검색을
 * 시도한다 — "아늑한 느낌" 같은 상품명에 그대로 없는 질의도 찾을 수 있다. 인덱스가 비어있거나
 * (gemini.api-key 미설정) 결과가 없으면 기존 LIKE 키워드 검색으로 폴백한다.
 */
@Component
@RequiredArgsConstructor
public class ProductSearchTool implements ChatTool {

    // 토큰/비용 방어: 한 번에 너무 많은 상품을 LLM 컨텍스트에 넘기지 않는다.
    private static final int MAX_RESULTS = 8;

    private final ProductService productService;
    private final GeminiEmbeddingClient embeddingClient;
    private final ProductEmbeddingIndex embeddingIndex;

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
        // "collection"은 Gemini에게 노출된 파라미터가 아니다 — 모델이 정하는 게 아니라, 어느
        // 페이지(채널)에서 온 요청인지에 따라 GeminiClient가 고정으로 주입하는 값이다
        // (ChatService/GeminiClient의 toolContext 참고).
        String collection = args.get("collection") != null ? args.get("collection").toString() : null;

        if (StringUtils.hasText(search) && !embeddingIndex.isEmpty()) {
            List<ProductEmbeddingIndex.ProductSummary> semanticResults = semanticSearch(search, categoryId, collection);
            if (!semanticResults.isEmpty()) {
                return semanticResults.stream()
                        .map(s -> Map.of(
                                "id", s.id(),
                                "name", s.name(),
                                "brand", s.brand() != null ? s.brand() : "",
                                "basePrice", s.basePrice(),
                                "categoryName", s.categoryName() != null ? s.categoryName() : ""
                        ))
                        .toList();
            }
            // 임베딩 검색이 결과를 못 찾으면(질의 임베딩 실패 포함) 아래 키워드 검색으로 폴백한다.
        }

        List<ProductDetailResponse> products = productService.getProducts(search, categoryId, collection);
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

    private List<ProductEmbeddingIndex.ProductSummary> semanticSearch(String search, Long categoryId, String collection) {
        return embeddingClient.embed(search, GeminiEmbeddingClient.TaskType.RETRIEVAL_QUERY)
                .map(queryVector -> embeddingIndex.search(queryVector, categoryId, collection, MAX_RESULTS))
                .orElse(List.of());
    }
}
