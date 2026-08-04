package com.jipdaum_spring.service;

import com.jipdaum_spring.domain.product.Product;
import com.jipdaum_spring.domain.product.ProductOption;
import com.jipdaum_spring.domain.product.ProductOptionRepository;
import com.jipdaum_spring.domain.product.ProductRepository;
import com.jipdaum_spring.dto.product.ProductOptionCreateRequest;
import com.jipdaum_spring.dto.product.ProductOptionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminProductOptionService {

    private final ProductOptionRepository productOptionRepository;
    private final ProductRepository productRepository;

    @Transactional
    public ProductOptionResponse create(Long productId, ProductOptionCreateRequest request) {
        Product product = findProduct(productId);
        ProductOption option = ProductOption.builder()
                .product(product)
                .optionName(request.optionName())
                .optionValue(request.optionValue())
                .extraPrice(request.extraPrice() != null ? request.extraPrice() : 0)
                .stockCount(request.stockCount() != null ? request.stockCount() : 0)
                .build();
        productOptionRepository.save(option);
        return ProductOptionResponse.from(option);
    }

    @Transactional
    public ProductOptionResponse update(Long productId, Long optionId, ProductOptionCreateRequest request) {
        ProductOption option = findOption(productId, optionId);
        option.update(
                request.optionName(),
                request.optionValue(),
                request.extraPrice() != null ? request.extraPrice() : 0,
                request.stockCount() != null ? request.stockCount() : 0
        );
        return ProductOptionResponse.from(option);
    }

    @Transactional
    public void delete(Long productId, Long optionId) {
        productOptionRepository.delete(findOption(productId, optionId));
    }

    public List<ProductOptionResponse> getAllByProduct(Long productId) {
        findProduct(productId);
        return productOptionRepository.findByProductId(productId).stream()
                .map(ProductOptionResponse::from)
                .toList();
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 상품입니다."));
    }

    private ProductOption findOption(Long productId, Long optionId) {
        ProductOption option = productOptionRepository.findById(optionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 옵션입니다."));
        if (!option.getProduct().getId().equals(productId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 상품의 옵션이 아닙니다.");
        }
        return option;
    }
}
