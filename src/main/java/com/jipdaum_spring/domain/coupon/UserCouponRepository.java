package com.jipdaum_spring.domain.coupon;

import com.jipdaum_spring.domain.jipdaumuser.JipdaumUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {
    List<UserCoupon> findByUserAndIsUsedFalse(JipdaumUser user);
    Optional<UserCoupon> findByUserAndCoupon_CodeAndIsUsedFalse(JipdaumUser user, String code);

    /**
     * 아직 사용되지 않은 경우에만 원자적으로 사용 처리한다. 같은 사용자의 동시 중복 요청으로
     * 1인 쿠폰이 두 번 소비되는 것을 막는다. 반환값이 0이면 이미 다른 요청이 먼저 사용한 것이다.
     */
    @Modifying
    @Query("UPDATE UserCoupon uc SET uc.isUsed = true, uc.usedAt = :usedAt " +
           "WHERE uc.id = :id AND uc.isUsed = false")
    int markUsedIfAvailable(@Param("id") Long id, @Param("usedAt") LocalDateTime usedAt);

    @Modifying
    @Query("UPDATE UserCoupon uc SET uc.isUsed = false, uc.usedAt = null " +
           "WHERE uc.user = :user AND uc.coupon = :coupon AND uc.isUsed = true")
    int markUnusedIfUsed(@Param("user") JipdaumUser user, @Param("coupon") Coupon coupon);
}
