package com.jipdaum_spring.domain.product;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "JIPDAUM_PRODUCT_OPTION")
@Getter
public class ProductOption {

    @Id
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
}
