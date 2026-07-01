package com.jipdaum_spring.domain.order;

import com.jipdaum_spring.domain.user.JipdaumUser;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "JIPDAUM_PAYMENT")
@Getter
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private JipdaumUser user;

    @Column(name = "merchant_uid", unique = true)
    private String merchantUid;

    @Column(name = "method")
    private String method;

    @Column(name = "status")
    private String status;

    @Column(name = "amount")
    private Integer amount;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Builder
    public Payment(Order order, JipdaumUser user, String method, String status, Integer amount) {
        this.order = order;
        this.user = user;
        this.merchantUid = UUID.randomUUID().toString();
        this.method = method;
        this.status = status;
        this.amount = amount;
        this.createdAt = LocalDateTime.now();
    }

    public void complete(String transactionId) {
        this.status = "SUCCESS";
        this.transactionId = transactionId;
        this.paidAt = LocalDateTime.now();
    }

    public String getMethodDisplay() {
        if (method == null) return null;
        return switch (method) {
            case "KAKAO" -> "카카오페이";
            case "NAVER" -> "네이버페이";
            case "TOSS"  -> "토스";
            case "CARD"  -> "신용카드";
            case "BANK"  -> "계좌이체";
            default -> method;
        };
    }
}
