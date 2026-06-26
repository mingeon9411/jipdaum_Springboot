package com.jipdaum_spring.repository;

import com.jipdaum_spring.domain.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}