package com.jipdaum_spring.service.chat;

import com.jipdaum_spring.dto.product.ProductDetailResponse;
import com.jipdaum_spring.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProductDetailTool implements ChatTool {

    private final ProductService productService;

    @Override
    public String name() {
        return "get_product_detail";
    }

    @Override
    public String description() {
        return "상품 ID로 집다움 상품의 상세 정보(설명, 브랜드, 가격, 카테고리)를 조회한다. "
                + "search_products로 먼저 id를 확인한 뒤 사용한다.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "productId", Map.of("type", "INTEGER", "description", "조회할 상품의 ID")
                ),
                "required", List.of("productId")
        );
    }

    @Override
    public Object execute(Map<String, Object> args) {
        Long productId = ChatToolArgs.asLong(args.get("productId"));
        if (productId == null) {
            return Map.of("error", "productId가 필요합니다.");
        }
        try {
            ProductDetailResponse product = productService.getProduct(productId);
            return Map.of(
                    "id", product.id(),
                    "name", product.name(),
                    "brand", product.brand() != null ? product.brand() : "",
                    "basePrice", product.basePrice(),
                    "description", product.description() != null ? product.description() : "",
                    "categoryName", product.categoryName() != null ? product.categoryName() : ""
            );
        } catch (ResponseStatusException e) {
            return Map.of("error", "해당 ID의 상품을 찾을 수 없습니다.");
        }
    }
}
