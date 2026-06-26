package com.jipdaum_spring.domain.order;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    public Order(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
        this.status = OrderStatus.PENDING;
    }

    public void markAsPaid() {
        this.status = OrderStatus.PAID;
    }
}
