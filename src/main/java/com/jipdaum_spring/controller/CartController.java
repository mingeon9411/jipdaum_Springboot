package com.jipdaum_spring.controller;

import com.jipdaum_spring.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/shop/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    public ResponseEntity<?> getCart() {
        return ResponseEntity.ok(cartService.getCart());
    }

    @PostMapping
    public ResponseEntity<?> addToCart(@RequestBody Map<String, Object> body) {
        log.info("POST /api/shop/cart 요청 수신 - body: {}", body);
        Map<String, Object> result = cartService.addToCart(body);
        boolean added = result.get("message").toString().contains("담겼습니다");
        log.info("POST /api/shop/cart 처리 완료 - 결과: {}", result);
        return ResponseEntity.status(added ? 201 : 200).body(result);
    }

    @PutMapping
    public ResponseEntity<?> updateCart(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(cartService.updateCart(body));
    }

    @DeleteMapping
    public ResponseEntity<?> deleteCart(@RequestBody Map<String, Object> body) {
        cartService.deleteCart(body);
        return ResponseEntity.noContent().build();
    }

    // 임시 진단용 - user_id 불일치 확인
    @GetMapping("/debug")
    public ResponseEntity<?> debugCart() {
        List<Map<String, Object>> cartAll = jdbcTemplate.queryForList(
            "SELECT id, user_id, product_id, quantity FROM JIPDAUM_CART ORDER BY user_id");
        List<Map<String, Object>> userAll = jdbcTemplate.queryForList(
            "SELECT id, email, username FROM JIPDAUM_USER WHERE email LIKE '%kakao%' OR username LIKE '%kakao%'");
        List<Map<String, Object>> cartUsers = jdbcTemplate.queryForList(
            "SELECT id, email, username FROM JIPDAUM_USER WHERE id IN (SELECT DISTINCT user_id FROM JIPDAUM_CART)");
        return ResponseEntity.ok(Map.of("cart_rows", cartAll, "kakao_users", userAll, "cart_user_details", cartUsers));
    }
}
