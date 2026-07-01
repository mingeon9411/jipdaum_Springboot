package com.jipdaum_spring.controller;

import com.jipdaum_spring.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/shop/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

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
    public ResponseEntity<?> createReview(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.status(201).body(productService.createReview(id, body));
    }
}
