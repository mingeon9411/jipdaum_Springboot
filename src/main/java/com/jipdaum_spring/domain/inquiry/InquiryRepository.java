package com.jipdaum_spring.domain.inquiry;

import com.jipdaum_spring.domain.jipdaumuser.JipdaumUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    List<Inquiry> findByUserOrderByCreatedAtDesc(JipdaumUser user);
}
