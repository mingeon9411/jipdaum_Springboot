package com.jipdaum_spring.domain.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategoryId(Long categoryId);

    @Query("SELECT p FROM Product p LEFT JOIN p.category cat WHERE " +
           "LOWER(p.name) LIKE :pattern OR " +
           "LOWER(p.brand) LIKE :pattern OR " +
           "LOWER(cat.name) LIKE :pattern")
    List<Product> searchByPattern(@Param("pattern") String pattern);

    @Query("SELECT p FROM Product p LEFT JOIN p.category cat WHERE p.category.id = :catId AND (" +
           "LOWER(p.name) LIKE :pattern OR " +
           "LOWER(p.brand) LIKE :pattern OR " +
           "LOWER(cat.name) LIKE :pattern)")
    List<Product> searchByPatternAndCategory(@Param("pattern") String pattern, @Param("catId") Long categoryId);
}
