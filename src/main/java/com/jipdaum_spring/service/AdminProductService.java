package com.jipdaum_spring.service;

import com.jipdaum_spring.domain.product.Category;
import com.jipdaum_spring.domain.product.CategoryRepository;
import com.jipdaum_spring.domain.product.Product;
import com.jipdaum_spring.domain.product.ProductRepository;
import com.jipdaum_spring.dto.product.ProductCreateRequest;
import com.jipdaum_spring.dto.product.ProductResponse;
import lombok.RequiredArgsConstructor;
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

    @Transactional
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
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse update(Long productId, ProductCreateRequest request) {
        Product product = findProduct(productId);
        Category category = findCategory(request.categoryId());
        product.update(category, request.name(), request.brand(), request.basePrice(),
                request.description(), request.thumbnailUrl());
        return ProductResponse.from(product);
    }

    @Transactional
    public void delete(Long productId) {
        productRepository.delete(findProduct(productId));
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