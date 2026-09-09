package com.jipdaum_spring.service;

import com.jipdaum_spring.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecentlyViewedService {
    private static final int MAX_ITEMS = 20;

    private final JdbcTemplate jdbc;
    private final CurrentUserProvider currentUserProvider;

    public record Item(long id, String name, int price, String image) {}

    @Transactional(readOnly = true)
    public List<Item> list() {
        long userId = currentUserProvider.getCurrentUser().getId();
        return jdbc.query("""
                SELECT p.id, p.name, p.base_price, p.thumbnail_url
                FROM JIPDAUM_RECENTLY_VIEWED r JOIN JIPDAUM_PRODUCT p ON p.id = r.product_id
                WHERE r.user_id = ? ORDER BY r.viewed_at DESC, r.id DESC LIMIT 20
                """, (rs, row) -> new Item(rs.getLong("id"), rs.getString("name"),
                rs.getInt("base_price"), rs.getString("thumbnail_url")), userId);
    }

    @Transactional
    public void view(long productId) {
        long userId = currentUserProvider.getCurrentUser().getId();
        if (jdbc.queryForList("SELECT id FROM JIPDAUM_PRODUCT WHERE id = ? FOR SHARE", Long.class, productId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다.");
        }
        jdbc.update("""
                INSERT INTO JIPDAUM_RECENTLY_VIEWED (user_id, product_id, viewed_at) VALUES (?, ?, CURRENT_TIMESTAMP(6))
                ON DUPLICATE KEY UPDATE viewed_at = CURRENT_TIMESTAMP(6)
                """, userId, productId);
        jdbc.update("""
                DELETE FROM JIPDAUM_RECENTLY_VIEWED WHERE user_id = ? AND id NOT IN (
                    SELECT id FROM (SELECT id FROM JIPDAUM_RECENTLY_VIEWED
                    WHERE user_id = ? ORDER BY viewed_at DESC, id DESC LIMIT 20) AS keep_items
                )
                """, userId, userId);
    }

    @Transactional
    public void merge(List<Long> productIds) {
        List<Long> distinctIds = productIds.stream().distinct().limit(MAX_ITEMS).toList();
        for (int i = distinctIds.size() - 1; i >= 0; i--) {
            view(distinctIds.get(i));
        }
    }

    @Transactional
    public void remove(long productId) {
        jdbc.update("DELETE FROM JIPDAUM_RECENTLY_VIEWED WHERE user_id = ? AND product_id = ?",
                currentUserProvider.getCurrentUser().getId(), productId);
    }

    @Transactional
    public void clear() {
        jdbc.update("DELETE FROM JIPDAUM_RECENTLY_VIEWED WHERE user_id = ?",
                currentUserProvider.getCurrentUser().getId());
    }
}
