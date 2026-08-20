package com.jipdaum_spring.domain.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategoryId(Long categoryId);

    List<Product> findByCollection(String collection);

    List<Product> findByCategoryIdAndCollection(Long categoryId, String collection);

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

    @Query("SELECT p FROM Product p LEFT JOIN p.category cat WHERE p.collection = :collection AND (" +
           "LOWER(p.name) LIKE :pattern OR " +
           "LOWER(p.brand) LIKE :pattern OR " +
           "LOWER(cat.name) LIKE :pattern)")
    List<Product> searchByPatternAndCollection(@Param("pattern") String pattern, @Param("collection") String collection);

    @Query("SELECT p FROM Product p LEFT JOIN p.category cat WHERE p.category.id = :catId AND p.collection = :collection AND (" +
           "LOWER(p.name) LIKE :pattern OR " +
           "LOWER(p.brand) LIKE :pattern OR " +
           "LOWER(cat.name) LIKE :pattern)")
    List<Product> searchByPatternAndCategoryAndCollection(@Param("pattern") String pattern, @Param("catId") Long categoryId,
                                                            @Param("collection") String collection);
}
