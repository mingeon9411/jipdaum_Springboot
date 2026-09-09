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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecentlyViewedIntegrationTest {
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
        String name = "recent-test-" + UUID.randomUUID();
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
    void persistsDeduplicatesIsolatesAndClears() throws Exception {
        mvc.perform(put("/api/shop/recently-viewed/{id}", productId).header("Authorization", first))
                .andExpect(status().isNoContent());
        mvc.perform(put("/api/shop/recently-viewed/{id}", productId).header("Authorization", first))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/shop/recently-viewed").header("Authorization", first))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(productId));
        mvc.perform(get("/api/shop/recently-viewed").header("Authorization", second))
                .andExpect(content().json("[]"));
        mvc.perform(delete("/api/shop/recently-viewed").header("Authorization", first))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/shop/recently-viewed").header("Authorization", first))
                .andExpect(content().json("[]"));
    }
}
