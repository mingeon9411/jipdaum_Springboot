package com.jipdaum_spring.domain.order;

import com.jipdaum_spring.domain.user.JipdaumUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserOrderByOrderDateDesc(JipdaumUser user);
    java.util.Optional<Order> findByIdAndUser(Long id, JipdaumUser user);
}
