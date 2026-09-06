package com.jipdaum_spring.service;

import com.jipdaum_spring.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {
    private final JdbcTemplate jdbc;
    private final CurrentUserProvider currentUserProvider;

    public record Item(long id, String name, String desc, int price, String image) {}

    @Transactional(readOnly = true)
    public List<Item> getWishlist() {
        long userId = currentUserProvider.getCurrentUser().getId();
        return jdbc.query("""
                SELECT p.id, p.name, p.description, p.base_price, p.thumbnail_url
                FROM JIPDAUM_WISHLIST w JOIN JIPDAUM_PRODUCT p ON p.id = w.product_id
                WHERE w.user_id = ? ORDER BY w.id DESC
                """, (rs, row) -> new Item(rs.getLong("id"), rs.getString("name"),
                rs.getString("description"), rs.getInt("base_price"), rs.getString("thumbnail_url")), userId);
    }

    @Transactional
    public void add(long productId) {
        long userId = currentUserProvider.getCurrentUser().getId();
        // Lock the product until insertion completes so a concurrent deletion cannot break the FK.
        if (jdbc.queryForList("SELECT id FROM JIPDAUM_PRODUCT WHERE id = ? FOR SHARE", Long.class, productId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다.");
        }
        jdbc.update("""
                INSERT INTO JIPDAUM_WISHLIST (user_id, product_id, created_at) VALUES (?, ?, CURRENT_TIMESTAMP(6))
                ON DUPLICATE KEY UPDATE id = id
                """, userId, productId);
    }

    @Transactional
    public void remove(long productId) {
        jdbc.update("DELETE FROM JIPDAUM_WISHLIST WHERE user_id = ? AND product_id = ?",
                currentUserProvider.getCurrentUser().getId(), productId);
    }
}
