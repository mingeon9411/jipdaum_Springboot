package com.jipdaum_spring.service;

import com.jipdaum_spring.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Uses the local migrated MySQL schema; all fixture changes roll back after each test. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WishlistIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenProvider tokens;
    String first;
    String second;
    long productId;

    @BeforeEach
    void setup() {
        first = createUser();
        second = createUser();
        productId = jdbc.queryForObject("SELECT MIN(id) FROM JIPDAUM_PRODUCT", Long.class);
    }

    String createUser() {
        String name = "wish-test-" + UUID.randomUUID();
        String email = name + "@example.invalid";
        jdbc.update("""
                INSERT INTO JIPDAUM_USER
                (username, email, nickname, password, first_name, last_name,
                 is_superuser, is_staff, is_active, is_email_verified, created_at)
                VALUES (?, ?, 'test', '!', '', '', 0, 0, 1, 1, CURRENT_TIMESTAMP(6))
                """, name, email);
        return "Bearer " + tokens.generateAccessToken(email);
    }

    @Test
    void persistsDeduplicatesAndIsolatesUsers() throws Exception {
        mvc.perform(put("/api/shop/wishlist/{id}", productId).header("Authorization", first))
                .andExpect(status().isNoContent());
        mvc.perform(put("/api/shop/wishlist/{id}", productId).header("Authorization", first))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/shop/wishlist").header("Authorization", first))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(productId))
                .andExpect(jsonPath("$[0].price").isNumber());
        mvc.perform(get("/api/shop/wishlist").header("Authorization", second))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(delete("/api/shop/wishlist/{id}", productId).header("Authorization", second))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/shop/wishlist").header("Authorization", first))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(delete("/api/shop/wishlist/{id}", productId).header("Authorization", first))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/shop/wishlist").header("Authorization", first))
                .andExpect(content().json("[]"));
    }

    @Test
    void rejectsAnonymousAndUnknownProducts() throws Exception {
        mvc.perform(get("/api/shop/wishlist")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/shop/wishlist/{id}", productId)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/shop/wishlist/{id}", productId)).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/shop/wishlist/9223372036854775807").header("Authorization", first))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/shop/wishlist").header("Authorization", first))
                .andExpect(content().json("[]"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM JIPDAUM_WISHLIST WHERE product_id = ?",
                Integer.class, Long.MAX_VALUE)).isZero();
    }
}
