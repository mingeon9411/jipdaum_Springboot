package com.jipdaum_spring.domain.product;

import com.jipdaum_spring.domain.jipdaumuser.JipdaumUser;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "JIPDAUM_REVIEW")
@Getter
@NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private JipdaumUser user;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "title")
    private String title;

    @Column(name = "comment", columnDefinition = "CLOB")
    private String comment;

    @Column(name = "review_image_url")
    private String reviewImageUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Builder
    public Review(Product product, JipdaumUser user, Integer rating, String title, String comment, String reviewImageUrl) {
        this.product = product;
        this.user = user;
        this.rating = rating;
        this.title = title != null ? title : "";
        this.comment = comment;
        this.reviewImageUrl = reviewImageUrl;
        this.createdAt = LocalDateTime.now();
    }
}
