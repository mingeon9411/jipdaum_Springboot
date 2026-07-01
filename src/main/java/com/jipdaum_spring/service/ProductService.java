package com.jipdaum_spring.service;

import com.jipdaum_spring.domain.product.*;
import com.jipdaum_spring.domain.user.JipdaumUser;
import com.jipdaum_spring.domain.user.JipdaumUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final JipdaumUserRepository jipdaumUserRepository;

    public List<Map<String, Object>> getProducts(String search, Long categoryId) {
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
        return products.stream().map(this::toProductMap).toList();
    }

    public Map<String, Object> getProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return toProductMap(product);
    }

    public List<Map<String, Object>> getReviews(Long productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("product", r.getProduct().getId());
                    m.put("user", r.getUser() != null ? r.getUser().getId() : null);
                    m.put("user_nickname", r.getUser() != null ? r.getUser().getNickname() : "");
                    m.put("rating", r.getRating());
                    m.put("comment", r.getComment());
                    m.put("review_image_url", r.getReviewImageUrl());
                    m.put("created_at", r.getCreatedAt());
                    return (Map<String, Object>) m;
                })
                .toList();
    }

    @Transactional
    public Map<String, Object> createReview(Long productId, Map<String, Object> body) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        JipdaumUser user = jipdaumUserRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Review review = Review.builder()
                .product(product)
                .user(user)
                .rating(Integer.parseInt(body.get("rating").toString()))
                .comment((String) body.get("comment"))
                .reviewImageUrl(body.get("review_image_url") != null ? body.get("review_image_url").toString() : null)
                .build();
        reviewRepository.save(review);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", review.getId());
        m.put("rating", review.getRating());
        m.put("comment", review.getComment());
        m.put("user_nickname", user.getNickname());
        m.put("created_at", review.getCreatedAt());
        return m;
    }

    private Map<String, Object> toProductMap(Product p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("name", p.getName());
        m.put("brand", p.getBrand());
        m.put("base_price", p.getBasePrice());
        m.put("description", p.getDescription());
        m.put("thumbnail_url", p.getThumbnailUrl());
        m.put("category", p.getCategory() != null ? p.getCategory().getId() : null);
        m.put("category_name", p.getCategory() != null ? p.getCategory().getName() : null);
        m.put("created_at", p.getCreatedAt());
        m.put("options", p.getOptions().stream().map(o -> {
            Map<String, Object> om = new LinkedHashMap<>();
            om.put("id", o.getId());
            om.put("option_name", o.getOptionName());
            om.put("option_value", o.getOptionValue());
            om.put("extra_price", o.getExtraPrice());
            om.put("stock_count", o.getStockCount());
            return om;
        }).toList());
        return m;
    }
}
