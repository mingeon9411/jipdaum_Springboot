package com.jipdaum_spring.service;

import com.jipdaum_spring.domain.order.Cart;
import com.jipdaum_spring.domain.order.CartRepository;
import com.jipdaum_spring.domain.product.Product;
import com.jipdaum_spring.domain.product.ProductOption;
import com.jipdaum_spring.domain.product.ProductOptionRepository;
import com.jipdaum_spring.domain.product.ProductRepository;
import com.jipdaum_spring.domain.user.JipdaumUser;
import com.jipdaum_spring.domain.user.JipdaumUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final JipdaumUserRepository jipdaumUserRepository;

    private JipdaumUser getCurrentUser() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<JipdaumUser> byEmail = jipdaumUserRepository.findByEmail(email);
        // principal에 @가 없으면 Spring Boot 형식 → Django 유저(이메일에 @ 포함)를 우선 탐색
        if (!email.contains("@")) {
            Optional<JipdaumUser> djangoUser = jipdaumUserRepository.findAllByUsernameIgnoreCase(email)
                    .stream()
                    .filter(u -> u.getEmail() != null && u.getEmail().contains("@"))
                    .findFirst();
            if (djangoUser.isPresent()) return djangoUser.get();
        }
        return byEmail.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCart() {
        JipdaumUser user = getCurrentUser();
        return cartRepository.findByUser(user).stream().map(item -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", item.getId());
            m.put("product_id", item.getProduct().getId());
            m.put("product_name", item.getProduct().getName());
            m.put("price", item.getProduct().getBasePrice());
            m.put("image", item.getProduct().getThumbnailUrl());
            m.put("option_id", item.getOption() != null ? item.getOption().getId() : null);
            m.put("option_name", item.getOption() != null
                    ? item.getOption().getOptionName() + ": " + item.getOption().getOptionValue()
                    : null);
            m.put("quantity", item.getQuantity());
            return (Map<String, Object>) m;
        }).toList();
    }

    @Transactional
    public Map<String, Object> addToCart(Map<String, Object> body) {
        JipdaumUser user = getCurrentUser();
        Long productId = Long.parseLong(body.get("product").toString());
        Object optionRaw = body.get("option") != null ? body.get("option") : body.get("product_option");
        Long optionId = optionRaw != null ? Long.parseLong(optionRaw.toString()) : null;
        int quantity = Integer.parseInt(body.getOrDefault("quantity", 1).toString());

        if (quantity < 1)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수량은 1개 이상이어야 합니다.");

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ProductOption option = optionId != null
                ? productOptionRepository.findById(optionId).orElseThrow()
                : null;

        Optional<Cart> existing = cartRepository.findByUserAndProductAndOption(user, product, option);
        if (existing.isPresent()) {
            Cart cart = existing.get();
            cart.setQuantity(cart.getQuantity() + quantity);
            cartRepository.save(cart);
            return Map.of("message", "장바구니 수량이 추가되었습니다.");
        }

        cartRepository.save(Cart.builder().user(user).product(product).option(option).quantity(quantity).build());
        return Map.of("message", "장바구니에 담겼습니다.");
    }

    @Transactional
    public Map<String, Object> updateCart(Map<String, Object> body) {
        JipdaumUser user = getCurrentUser();
        Long itemId = Long.parseLong(body.get("item_id").toString());
        int quantity = Integer.parseInt(body.get("quantity").toString());

        if (quantity < 1)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수량은 1개 이상이어야 합니다.");

        Cart cart = cartRepository.findByIdAndUser(itemId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        cart.setQuantity(quantity);
        cartRepository.save(cart);
        return Map.of("message", "수량이 수정되었습니다.");
    }

    @Transactional
    public void deleteCart(Map<String, Object> body) {
        JipdaumUser user = getCurrentUser();
        Long itemId = Long.parseLong(body.get("item_id").toString());
        Cart cart = cartRepository.findByIdAndUser(itemId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        cartRepository.delete(cart);
    }
}
