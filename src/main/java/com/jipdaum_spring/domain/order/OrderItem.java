package com.jipdaum_spring.domain.order;

import com.jipdaum_spring.domain.product.Product;
import com.jipdaum_spring.domain.product.ProductOption;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "JIPDAUM_ORDER_ITEM")
@Getter
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id")
    private ProductOption option;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "ordered_price")
    private Integer orderedPrice;

    @Builder
    public OrderItem(Order order, Product product, ProductOption option, Integer quantity, Integer orderedPrice) {
        this.order = order;
        this.product = product;
        this.option = option;
        this.quantity = quantity;
        this.orderedPrice = orderedPrice;
    }
}
