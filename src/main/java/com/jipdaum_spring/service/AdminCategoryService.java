package com.jipdaum_spring.service;

import com.jipdaum_spring.domain.product.Category;
import com.jipdaum_spring.domain.product.CategoryRepository;
import com.jipdaum_spring.dto.product.CategoryCreateRequest;
import com.jipdaum_spring.dto.product.CategoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponse create(CategoryCreateRequest request) {
        Category parent = findParent(request.parentId());
        Category category = Category.builder()
                .name(request.name())
                .parent(parent)
                .build();
        categoryRepository.save(category);
        return CategoryResponse.from(category);
    }

    @Transactional
    public CategoryResponse update(Long categoryId, CategoryCreateRequest request) {
        Category category = findCategory(categoryId);
        if (request.parentId() != null && request.parentId().equals(categoryId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "카테고리는 자기 자신을 상위 카테고리로 가질 수 없습니다.");
        }
        Category parent = findParent(request.parentId());
        category.update(request.name(), parent);
        return CategoryResponse.from(category);
    }

    @Transactional
    public void delete(Long categoryId) {
        categoryRepository.delete(findCategory(categoryId));
    }

    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 카테고리입니다."));
    }

    private Category findParent(Long parentId) {
        if (parentId == null) return null;
        return findCategory(parentId);
    }
}
