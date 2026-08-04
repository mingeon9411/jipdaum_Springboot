package com.jipdaum_spring.domain.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductOptionRepository extends JpaRepository<ProductOption, Long> {

    List<ProductOption> findByProductId(Long productId);

    /**
     * 재고가 충분한 경우에만 원자적으로 차감한다. 동시에 결제가 완료되는 여러 주문이
     * 같은 옵션의 재고를 함께 소진해도 DB 레벨 조건부 UPDATE로 음수가 되지 않게 막는다.
     * 반환값이 0이면 그 시점에 재고가 부족했다는 뜻이다.
     */
    @Modifying
    @Query("UPDATE ProductOption o SET o.stockCount = o.stockCount - :qty " +
           "WHERE o.id = :id AND o.stockCount >= :qty")
    int decrementStockIfAvailable(@Param("id") Long id, @Param("qty") int qty);
}
