package com.jipdaum_spring.controller;

import com.jipdaum_spring.dto.product.ProductOptionCreateRequest;
import com.jipdaum_spring.dto.product.ProductOptionResponse;
import com.jipdaum_spring.service.AdminProductOptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/products/{productId}/options")
@RequiredArgsConstructor
public class AdminProductOptionController {

    private final AdminProductOptionService adminProductOptionService;

    @PostMapping
    public ResponseEntity<ProductOptionResponse> create(
            @PathVariable Long productId,
            @Valid @RequestBody ProductOptionCreateRequest request
    ) {
        ProductOptionResponse response = adminProductOptionService.create(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{optionId}")
    public ResponseEntity<ProductOptionResponse> update(
            @PathVariable Long productId,
            @PathVariable Long optionId,
            @Valid @RequestBody ProductOptionCreateRequest request
    ) {
        return ResponseEntity.ok(adminProductOptionService.update(productId, optionId, request));
    }

    @DeleteMapping("/{optionId}")
    public ResponseEntity<Void> delete(@PathVariable Long productId, @PathVariable Long optionId) {
        adminProductOptionService.delete(productId, optionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<ProductOptionResponse>> getAll(@PathVariable Long productId) {
        return ResponseEntity.ok(adminProductOptionService.getAllByProduct(productId));
    }
}
