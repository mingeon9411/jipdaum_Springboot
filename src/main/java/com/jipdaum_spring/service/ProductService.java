package com.jipdaum_spring.service;

import com.jipdaum_spring.domain.product.*;
import com.jipdaum_spring.domain.jipdaumuser.JipdaumUser;
import com.jipdaum_spring.dto.product.CategoryResponse;
import com.jipdaum_spring.dto.product.CreateReviewRequest;
import com.jipdaum_spring.dto.product.CreateReviewResponse;
import com.jipdaum_spring.dto.product.ProductDetailResponse;
import com.jipdaum_spring.dto.product.ReviewResponse;
import com.jipdaum_spring.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final CategoryRepository categoryRepository;
    private final CurrentUserProvider currentUserProvider;

    public List<CategoryResponse> getCategories() {
        return categoryRepository.findByParentIsNull().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public List<ProductDetailResponse> getProducts(String search, Long categoryId) {
        List<Product> products;
        if (search != null && !search.isBlank()) {
            // 검색어를 공백으로 분리해 각 토큰을 개별 검색 후 합산 (중복 제거)
            String[] tokens = search.trim().split("\\s+");
            Set<Product> resultSet = new LinkedHashSet<>();
            for (String token : tokens) {
                if (token.length() < 1) continue;
                String pattern = "%" + token.toLowerCase() + "%";
                if (categoryId != null) {
                    resultSet.addAll(productRepository.searchByPatternAndCategory(pattern, categoryId));
                } else {
                    resultSet.addAll(productRepository.searchByPattern(pattern));
                }
            }
            products = new ArrayList<>(resultSet);
        } else if (categoryId != null) {
            products = productRepository.findByCategoryId(categoryId);
        } else {
            products = productRepository.findAll();
        }
        return products.stream().map(ProductDetailResponse::from).toList();
    }

    public ProductDetailResponse getProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."));
        return ProductDetailResponse.from(product);
    }

    public List<ReviewResponse> getReviews(Long productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(ReviewResponse::from)
                .toList();
    }

    @Transactional
    public CreateReviewResponse createReview(Long productId, CreateReviewRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."));

        JipdaumUser user = currentUserProvider.getCurrentUser();

        Review review = Review.builder()
                .product(product)
                .user(user)
                .rating(request.rating())
                .comment(request.comment())
                .reviewImageUrl(request.reviewImageUrl())
                .build();
        reviewRepository.save(review);

        return CreateReviewResponse.of(review, user.getNickname());
    }
}