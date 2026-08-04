package com.jipdaum_spring.controller;

import com.jipdaum_spring.dto.product.CategoryCreateRequest;
import com.jipdaum_spring.dto.product.CategoryResponse;
import com.jipdaum_spring.service.AdminCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryCreateRequest request) {
        CategoryResponse response = adminCategoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> update(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryCreateRequest request
    ) {
        return ResponseEntity.ok(adminCategoryService.update(categoryId, request));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> delete(@PathVariable Long categoryId) {
        adminCategoryService.delete(categoryId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAll() {
        return ResponseEntity.ok(adminCategoryService.getAll());
    }
}
