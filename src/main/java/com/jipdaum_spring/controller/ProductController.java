package com.jipdaum_spring.controller;

import com.jipdaum_spring.dto.product.CreateReviewRequest;
import com.jipdaum_spring.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/categories")
    public ResponseEntity<?> listCategories() {
        return ResponseEntity.ok(productService.getCategories());
    }

    @GetMapping
    public ResponseEntity<?> listProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long category
    ) {
        return ResponseEntity.ok(productService.getProducts(search, category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<?> getReviews(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getReviews(id));
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<?> createReview(@PathVariable Long id, @Valid @RequestBody CreateReviewRequest request) {
        return ResponseEntity.status(201).body(productService.createReview(id, request));
    }
}
