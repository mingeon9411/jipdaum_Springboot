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
        return getProducts(search, categoryId, null);
    }

    /**
     * collection이 주어지면("main"/"korean_hall") 그 진열에 속한 상품만 대상으로 한다 — 챗봇이
     * 페이지 문맥(메인 쇼핑몰 vs 한국관)에 맞는 상품만 추천하도록 ProductSearchTool이 사용한다.
     * 공개 상품 목록 API(2-arg 오버로드)는 기존과 동일하게 collection 구분 없이 전체를 보여준다.
     */
    public List<ProductDetailResponse> getProducts(String search, Long categoryId, String collection) {
        List<Product> products;
        if (search != null && !search.isBlank()) {
            // 검색어를 공백으로 분리해 각 토큰을 개별 검색 후 합산 (중복 제거)
            String[] tokens = search.trim().split("\\s+");
            Set<Product> resultSet = new LinkedHashSet<>();
            for (String token : tokens) {
                if (token.length() < 1) continue;
                String pattern = "%" + token.toLowerCase() + "%";
                if (categoryId != null && collection != null) {
                    resultSet.addAll(productRepository.searchByPatternAndCategoryAndCollection(pattern, categoryId, collection));
                } else if (categoryId != null) {
                    resultSet.addAll(productRepository.searchByPatternAndCategory(pattern, categoryId));
                } else if (collection != null) {
                    resultSet.addAll(productRepository.searchByPatternAndCollection(pattern, collection));
                } else {
                    resultSet.addAll(productRepository.searchByPattern(pattern));
                }
            }
            products = new ArrayList<>(resultSet);
        } else if (categoryId != null && collection != null) {
            products = productRepository.findByCategoryIdAndCollection(categoryId, collection);
        } else if (categoryId != null) {
            products = productRepository.findByCategoryId(categoryId);
        } else if (collection != null) {
            products = productRepository.findByCollection(collection);
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

    // 갤러리(룩북) 패널용 — 상품 상관없이 사진 첨부된 최신 리뷰만 모아서 보여준다.
    public List<ReviewResponse> getPhotoReviews() {
        return reviewRepository.findTop100ByReviewImageUrlIsNotNullOrderByCreatedAtDesc().stream()
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
                .title(request.title())
                .comment(request.comment())
                .reviewImageUrl(request.reviewImageUrl())
                .build();
        reviewRepository.save(review);

        return CreateReviewResponse.of(review, user.getNickname());
    }
}