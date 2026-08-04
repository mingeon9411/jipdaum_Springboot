package com.jipdaum_spring.domain.product;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "JIPDAUM_PRODUCT_OPTION")
@Getter
@NoArgsConstructor
public class ProductOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "option_name")
    private String optionName;

    @Column(name = "option_value")
    private String optionValue;

    @Column(name = "extra_price")
    private Integer extraPrice;

    @Column(name = "stock_count")
    private Integer stockCount;

    @Builder
    public ProductOption(Product product, String optionName, String optionValue, Integer extraPrice, Integer stockCount) {
        this.product = product;
        this.optionName = optionName;
        this.optionValue = optionValue;
        this.extraPrice = extraPrice;
        this.stockCount = stockCount;
    }

    public ProductOption update(String optionName, String optionValue, Integer extraPrice, Integer stockCount) {
        this.optionName = optionName;
        this.optionValue = optionValue;
        this.extraPrice = extraPrice;
        this.stockCount = stockCount;
        return this;
    }
}
