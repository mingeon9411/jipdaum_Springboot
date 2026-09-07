package com.jipdaum_spring.domain.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);

    // 갤러리(룩북) 패널에 상품 무관하게 최신 포토리뷰를 보여주기 위한 조회.
    // 무제한 조회를 막기 위해 최근 100건으로 제한한다.
    List<Review> findTop100ByReviewImageUrlIsNotNullOrderByCreatedAtDesc();

    List<Review> findTop100ByOrderByCreatedAtDesc();
}
