package com.jipdaum_spring.domain.product;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "JIPDAUM_PRODUCT")
@Getter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "name")
    private String name;

    @Column(name = "brand")
    private String brand;


    @Column(name = "base_price")
    private Integer basePrice;

    @Column(name = "description", columnDefinition = "CLOB")
    private String description;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    // Django Products.models.Product.COLLECTION_CHOICES와 매칭 — "main"(기본 진열) 또는
    // "korean_hall"(한국관 진열). 카테고리명이 두 진열에서 겹쳐서(둘 다 소파/테이블/조명...) 이
    // 필드로만 어느 진열 소속인지 구분할 수 있다. 챗봇 상품 검색(ProductSearchTool)이 페이지
    // 문맥에 맞는 상품만 찾도록 필터링할 때 쓴다.
    @Column(name = "collection")
    private String collection;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<ProductOption> options = new ArrayList<>();

    @Builder
    public Product(Category category, String name, String brand, Integer basePrice, String description, String thumbnailUrl) {
        this.category = category;
        this.name = name;
        this.brand = brand;
        this.basePrice = basePrice;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.createdAt = LocalDateTime.now();
    }

    public Product update(Category category, String name, String brand, Integer basePrice, String description, String thumbnailUrl) {
        this.category = category;
        this.name = name;
        this.brand = brand;
        this.basePrice = basePrice;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        return this;
    }
}