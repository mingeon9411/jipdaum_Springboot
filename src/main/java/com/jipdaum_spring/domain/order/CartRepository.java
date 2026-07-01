package com.jipdaum_spring.domain.order;

import com.jipdaum_spring.domain.product.Product;
import com.jipdaum_spring.domain.product.ProductOption;
import com.jipdaum_spring.domain.user.JipdaumUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    List<Cart> findByUser(JipdaumUser user);
    Optional<Cart> findByUserAndProductAndOption(JipdaumUser user, Product product, ProductOption option);
    Optional<Cart> findByIdAndUser(Long id, JipdaumUser user);
}
