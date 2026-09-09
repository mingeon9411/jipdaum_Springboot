package com.jipdaum_spring.controller;

import com.jipdaum_spring.dto.recent.RecentlyViewedMergeRequest;
import com.jipdaum_spring.service.RecentlyViewedService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop/recently-viewed")
@RequiredArgsConstructor
public class RecentlyViewedController {
    private final RecentlyViewedService recentlyViewedService;

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(recentlyViewedService.list());
    }

    @PutMapping("/{productId}")
    public ResponseEntity<Void> view(@PathVariable long productId) {
        recentlyViewedService.view(productId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping
    public ResponseEntity<Void> merge(@Valid @RequestBody RecentlyViewedMergeRequest request) {
        recentlyViewedService.merge(request.productIds());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> remove(@PathVariable long productId) {
        recentlyViewedService.remove(productId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clear() {
        recentlyViewedService.clear();
        return ResponseEntity.noContent().build();
    }
}
