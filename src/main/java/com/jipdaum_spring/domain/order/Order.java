package com.jipdaum_spring.domain.order;

import com.jipdaum_spring.domain.coupon.Coupon;
import com.jipdaum_spring.domain.jipdaumuser.JipdaumUser;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "JIPDAUM_ORDER")
@Getter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private JipdaumUser user;

    @Column(name = "total_amount")
    private Integer totalAmount;

    @Column(name = "discount_amount")
    private Integer discountAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @Column(name = "status")
    private String status;

    @Column(name = "shipping_addr")
    private String shippingAddr;

    @Column(name = "order_date")
    private LocalDateTime orderDate;

    @Column(name = "carrier")
    private String carrier = "";

    @Column(name = "tracking_number")
    private String trackingNumber = "";

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", fetch = FetchType.LAZY)
    private Payment payment;

    @Builder
    public Order(JipdaumUser user, Integer totalAmount, Integer discountAmount,
                 Coupon coupon, String status, String shippingAddr) {
        this.user = user;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.coupon = coupon;
        this.status = status;
        this.shippingAddr = shippingAddr;
        this.orderDate = LocalDateTime.now();
    }

    public void complete() {
        this.status = "ORDERED";
    }

    public void cancel() {
        this.status = "CANCELLED";
    }

    public void assignTracking(String carrier, String trackingNumber) {
        this.carrier = carrier;
        this.trackingNumber = trackingNumber;
    }

    public String getStatusDisplay() {
        if (status == null) return "";
        return switch (status) {
            case "PENDING"   -> "결제대기";
            case "ORDERED"   -> "주문완료(결제완료)";
            case "SHIPPED"   -> "배송중";
            case "DELIVERED" -> "배송완료";
            case "CANCELLED" -> "주문취소";
            default -> status;
        };
    }
}
