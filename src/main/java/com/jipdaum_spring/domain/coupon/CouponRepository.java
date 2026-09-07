package com.jipdaum_spring.domain.coupon;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCodeAndIsPersonalFalse(String code);
    Optional<Coupon> findByCode(String code);
    List<Coupon> findAllByCodeInAndIsActiveTrue(List<String> codes);

    /**
     * usageLimit을 넘지 않는 경우에만 원자적으로 usedCount를 1 증가시킨다.
     * 동시에 여러 주문이 같은 쿠폰을 사용해도 usageLimit을 초과하지 않도록 DB 레벨에서 조건부로 갱신한다.
     * 반환값이 0이면(조건 불충족) 이미 소진된 것이므로 호출한 쪽에서 실패로 처리해야 한다.
     */
    @Modifying
    @Query("UPDATE Coupon c SET c.usedCount = c.usedCount + 1 " +
           "WHERE c.id = :id AND (c.usageLimit IS NULL OR c.usedCount < c.usageLimit)")
    int incrementUsedCountIfAvailable(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Coupon c SET c.usedCount = c.usedCount - 1 WHERE c.id = :id AND c.usedCount > 0")
    int decrementUsedCountIfPositive(@Param("id") Long id);
}
