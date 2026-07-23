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