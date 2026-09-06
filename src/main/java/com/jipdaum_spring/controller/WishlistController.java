package com.jipdaum_spring.controller;

import com.jipdaum_spring.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/shop/wishlist")
@RequiredArgsConstructor
public class WishlistController {
    private final WishlistService wishlistService;

    @GetMapping
    public List<WishlistService.Item> list() { return wishlistService.getWishlist(); }

    @PutMapping("/{productId}")
    public ResponseEntity<Void> add(@PathVariable long productId) {
        wishlistService.add(productId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> remove(@PathVariable long productId) {
        wishlistService.remove(productId);
        return ResponseEntity.noContent().build();
    }
}
