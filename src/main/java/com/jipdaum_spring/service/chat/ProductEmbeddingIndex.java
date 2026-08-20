package com.jipdaum_spring.service.chat;

import com.jipdaum_spring.domain.product.Product;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 상품 임베딩을 메모리에만 보관하는 브루트포스 벡터 인덱스(코사인 유사도).
 * Django가 소유한 MySQL 스키마를 건드리지 않기 위해 영속화하지 않는다 — 재시작하면 다시
 * 색인하면 그만일 만큼 카탈로그가 작다(이 클래스 도입 시점 기준 상품 9개). 수만 개로 늘어나도
 * 브루트포스 코사인 유사도는 충분히 빠르다 — ANN 인덱스가 필요해지는 건 훨씬 더 큰 규모부터다.
 */
@Component
public class ProductEmbeddingIndex {

    public record ProductSummary(
            Long id, String name, String brand, Integer basePrice, String categoryName, Long categoryId, String collection
    ) {
        public static ProductSummary from(Product p) {
            return new ProductSummary(
                    p.getId(),
                    p.getName(),
                    p.getBrand(),
                    p.getBasePrice(),
                    p.getCategory() != null ? p.getCategory().getName() : null,
                    p.getCategory() != null ? p.getCategory().getId() : null,
                    p.getCollection()
            );
        }
    }

    private record Entry(float[] vector, ProductSummary summary) {
    }

    private final Map<Long, Entry> entries = new ConcurrentHashMap<>();

    public void upsert(Long productId, float[] vector, ProductSummary summary) {
        entries.put(productId, new Entry(vector, summary));
    }

    public void remove(Long productId) {
        entries.remove(productId);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int size() {
        return entries.size();
    }

    /**
     * 코사인 유사도 상위 topK. categoryId가 주어지면 그 카테고리 상품만, collection이 주어지면
     * 그 진열(메인/한국관) 상품만 대상으로 한다.
     */
    public List<ProductSummary> search(float[] queryVector, Long categoryId, String collection, int topK) {
        return entries.values().stream()
                .filter(e -> categoryId == null || categoryId.equals(e.summary().categoryId()))
                .filter(e -> collection == null || collection.equals(e.summary().collection()))
                .map(e -> Map.entry(e.summary(), cosineSimilarity(queryVector, e.vector())))
                .sorted(Map.Entry.<ProductSummary, Float>comparingByValue().reversed())
                .limit(topK)
                .map(Map.Entry::getKey)
                .toList();
    }

    private static float cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            // 임베딩 모델이 도중에 바뀌어 차원이 안 맞는 경우(운영 중 모델 교체 등) 방어.
            return -1f;
        }
        float dot = 0f;
        float normA = 0f;
        float normB = 0f;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0f || normB == 0f) {
            return 0f;
        }
        return (float) (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
    }
}
