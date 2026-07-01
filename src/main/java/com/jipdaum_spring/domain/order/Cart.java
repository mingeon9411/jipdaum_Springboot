package com.jipdaum_spring.domain.order;

import com.jipdaum_spring.domain.product.Product;
import com.jipdaum_spring.domain.product.ProductOption;
import com.jipdaum_spring.domain.user.JipdaumUser;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "JIPDAUM_CART")
@Getter
@Setter
@NoArgsConstructor
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private JipdaumUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id")
    private ProductOption option;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Builder
    public Cart(JipdaumUser user, Product product, ProductOption option, Integer quantity) {
        this.user = user;
        this.product = product;
        this.option = option;
        this.quantity = quantity;
        this.createdAt = LocalDateTime.now();
    }
}
