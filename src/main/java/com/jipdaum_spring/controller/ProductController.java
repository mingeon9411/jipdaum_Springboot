package com.jipdaum_spring.controller;

import com.jipdaum_spring.dto.product.CreateReviewRequest;
import com.jipdaum_spring.service.ProductService;
import com.jipdaum_spring.service.chat.ProductSearchTool;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductSearchTool productSearchTool;

    @GetMapping("/categories")
    public ResponseEntity<?> listCategories() {
        return ResponseEntity.ok(productService.getCategories());
    }

    @GetMapping("/semantic-search")
    public ResponseEntity<?> semanticSearch(@RequestParam String q) {
        return ResponseEntity.ok(productSearchTool.search(q, null, "main"));
    }

    // collection("main"/"korean_hall")을 넘기면 그 진열 상품만 필터링해서 돌려준다 — 안 넘기면
    // 기존과 동일하게 전체를 보여준다(하위 호환). 프론트가 아직 이 값을 실제 화면에 merge하진
    // 않음 — DB 시드 데이터가 현재 상품 라인업과 안 맞아서 재입력 전까지는 조회만 가능하게 열어둠.
    @GetMapping
    public ResponseEntity<?> listProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) String collection
    ) {
        return ResponseEntity.ok(productService.getProducts(search, category, collection));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<?> getReviews(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getReviews(id));
    }

    // 룩북 갤러리 패널용 — 상품 상관없이 사진 첨부된 최신 리뷰만 모아서 보여준다.
    // "/{id}"보다 세그먼트가 많은 리터럴 경로라 라우팅 충돌 없음.
    @GetMapping("/reviews/photos")
    public ResponseEntity<?> getPhotoReviews() {
        return ResponseEntity.ok(productService.getPhotoReviews());
    }

    @GetMapping("/reviews/recent")
    public ResponseEntity<?> getRecentReviews() {
        return ResponseEntity.ok(productService.getRecentReviews());
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<?> createReview(@PathVariable Long id, @Valid @RequestBody CreateReviewRequest request) {
        return ResponseEntity.status(201).body(productService.createReview(id, request));
    }
}
