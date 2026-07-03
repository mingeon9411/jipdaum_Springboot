package com.jipdaum_spring.controller;

import com.jipdaum_spring.dto.cart.AddToCartRequest;
import com.jipdaum_spring.dto.cart.CartMutationResult;
import com.jipdaum_spring.dto.cart.DeleteCartRequest;
import com.jipdaum_spring.dto.cart.UpdateCartRequest;
import com.jipdaum_spring.dto.common.MessageResponse;
import com.jipdaum_spring.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<?> getCart() {
        return ResponseEntity.ok(cartService.getCart());
    }

    @PostMapping
    public ResponseEntity<?> addToCart(@Valid @RequestBody AddToCartRequest request) {
        CartMutationResult result = cartService.addToCart(request);
        return ResponseEntity.status(result.created() ? 201 : 200)
                .body(new MessageResponse(result.message()));
    }

    @PutMapping
    public ResponseEntity<?> updateCart(@Valid @RequestBody UpdateCartRequest request) {
        cartService.updateCart(request);
        return ResponseEntity.ok(new MessageResponse("수량이 수정되었습니다."));
    }

    @DeleteMapping
    public ResponseEntity<?> deleteCart(@Valid @RequestBody DeleteCartRequest request) {
        cartService.deleteCart(request);
        return ResponseEntity.noContent().build();
    }
}