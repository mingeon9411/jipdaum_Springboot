package com.jipdaum_spring.service.chat;

import com.jipdaum_spring.domain.product.Product;
import com.jipdaum_spring.domain.product.ProductRepository;
import com.jipdaum_spring.service.GeminiEmbeddingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 부팅 완료 시 전체 상품을 1회 색인한다(batchEmbedContents로 한 번의 API 호출에 처리).
 * gemini.api-key가 비어있으면 GeminiEmbeddingClient가 항상 empty를 반환하므로, 이 경우
 * 색인은 그냥 아무것도 안 하고 끝난다 — ProductEmbeddingIndex가 비어있으면 ProductSearchTool이
 * 자동으로 기존 키워드 검색을 쓰므로 로컬/테스트 환경에는 영향이 없다.
 *
 * 이후 추가/수정/삭제는 AdminProductService가 건건이 색인을 갱신한다(여기서는 최초 1회뿐).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEmbeddingIndexInitializer {

    private final ProductRepository productRepository;
    private final GeminiEmbeddingClient embeddingClient;
    private final ProductEmbeddingIndex index;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true) // product.getCategory() lazy 로딩을 위해 세션을 열어둔다.
    public void reindexAll() {
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            return;
        }

        List<String> texts = products.stream().map(ProductEmbeddingIndexInitializer::toEmbeddingText).toList();
        List<Optional<float[]>> vectors = embeddingClient.embedBatch(texts, GeminiEmbeddingClient.TaskType.RETRIEVAL_DOCUMENT);

        int indexed = 0;
        for (int i = 0; i < products.size(); i++) {
            Optional<float[]> vector = vectors.get(i);
            if (vector.isPresent()) {
                Product product = products.get(i);
                index.upsert(product.getId(), vector.get(), ProductEmbeddingIndex.ProductSummary.from(product));
                indexed++;
            }
        }

        if (indexed == 0) {
            log.info("상품 임베딩 색인을 만들지 못했습니다(gemini.api-key 미설정 또는 호출 실패) — 챗봇은 기존 키워드 검색으로 동작합니다.");
        } else {
            log.info("상품 임베딩 색인 완료: {}/{}개", indexed, products.size());
        }
    }

    public static String toEmbeddingText(Product p) {
        StringBuilder sb = new StringBuilder(p.getName() != null ? p.getName() : "");
        if (p.getBrand() != null) {
            sb.append(' ').append(p.getBrand());
        }
        if (p.getCategory() != null && p.getCategory().getName() != null) {
            sb.append(' ').append(p.getCategory().getName());
        }
        if (p.getDescription() != null) {
            sb.append(' ').append(p.getDescription());
        }
        return sb.toString();
    }
}
