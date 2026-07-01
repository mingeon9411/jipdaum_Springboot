package com.jipdaum_spring.domain.coupon;

import com.jipdaum_spring.domain.user.JipdaumUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {
    List<UserCoupon> findByUserAndIsUsedFalse(JipdaumUser user);
    Optional<UserCoupon> findByUserAndCoupon_CodeAndIsUsedFalse(JipdaumUser user, String code);
}
