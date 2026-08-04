package com.jipdaum_spring.domain.coupon;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "JIPDAUM_COUPON")
@Getter
@Setter
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", unique = true)
    private String code;

    @Column(name = "name")
    private String name;

    @Column(name = "discount_type")
    private String discountType;

    @Column(name = "discount_value")
    private Integer discountValue;

    @Column(name = "min_order_amount")
    private Integer minOrderAmount;

    @Column(name = "max_discount_amount")
    private Integer maxDiscountAmount;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "used_count")
    private Integer usedCount;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "is_personal")
    private Boolean isPersonal;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public boolean isValid() {
        if (Boolean.FALSE.equals(isActive)) return false;
        if (expiryDate != null && expiryDate.isBefore(LocalDate.now())) return false;
        if (usageLimit != null && usedCount != null && usedCount >= usageLimit) return false;
        return true;
    }

    public int calcDiscount(int orderAmount) {
        if (minOrderAmount != null && orderAmount < minOrderAmount) return 0;
        if ("FIXED".equals(discountType)) {
            return Math.min(discountValue != null ? discountValue : 0, orderAmount);
        }
        int discount = (int) (orderAmount * (discountValue != null ? discountValue : 0) / 100.0);
        if (maxDiscountAmount != null) discount = Math.min(discount, maxDiscountAmount);
        return discount;
    }
}