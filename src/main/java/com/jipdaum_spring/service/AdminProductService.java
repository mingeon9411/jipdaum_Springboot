package com.jipdaum_spring.service;

import com.jipdaum_spring.domain.product.Category;
import com.jipdaum_spring.domain.product.CategoryRepository;
import com.jipdaum_spring.domain.product.Product;
import com.jipdaum_spring.domain.product.ProductRepository;
import com.jipdaum_spring.dto.product.ProductCreateRequest;
import com.jipdaum_spring.dto.product.ProductResponse;
import com.jipdaum_spring.service.chat.ProductEmbeddingIndex;
import com.jipdaum_spring.service.chat.ProductEmbeddingIndexInitializer;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final GeminiEmbeddingClient embeddingClient;
    private final ProductEmbeddingIndex embeddingIndex;

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse create(ProductCreateRequest request) {
        Category category = findCategory(request.categoryId());
        Product product = Product.builder()
                .category(category)
                .name(request.name())
                .brand(request.brand())
                .basePrice(request.basePrice())
                .description(request.description())
                .thumbnailUrl(request.thumbnailUrl())
                .build();
        productRepository.save(product);
        reindex(product);
        return ProductResponse.from(product);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse update(Long productId, ProductCreateRequest request) {
        Product product = findProduct(productId);
        Category category = findCategory(request.categoryId());
        product.update(category, request.name(), request.brand(), request.basePrice(),
                request.description(), request.thumbnailUrl());
        reindex(product);
        return ProductResponse.from(product);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void delete(Long productId) {
        productRepository.delete(findProduct(productId));
        embeddingIndex.remove(productId);
    }

    /**
     * 챗봇 상품 검색용 임베딩 색인을 이 상품 한 건만 갱신한다(전체 재색인 없이). gemini.api-key가
     * 없으면 embed()가 빈 값을 반환하므로 그냥 조용히 스킵된다 — 상품 저장 자체는 항상 성공한다.
     */
    private void reindex(Product product) {
        String text = ProductEmbeddingIndexInitializer.toEmbeddingText(product);
        embeddingClient.embed(text, GeminiEmbeddingClient.TaskType.RETRIEVAL_DOCUMENT)
                .ifPresent(vector -> embeddingIndex.upsert(
                        product.getId(), vector, ProductEmbeddingIndex.ProductSummary.from(product)));
    }

    public Page<ProductResponse> getAll(Pageable pageable) {
        return productRepository.findAll(pageable).map(ProductResponse::from);
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 상품입니다."));
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 카테고리입니다."));
    }
}