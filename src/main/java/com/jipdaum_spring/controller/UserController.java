package com.jipdaum_spring.controller;

import com.jipdaum_spring.domain.User.User;
import com.jipdaum_spring.domain.User.UserRepository;
import com.jipdaum_spring.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        List<Map<String, Object>> users = userRepository.findAll().stream()
                .map(user -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", user.getId());
                    map.put("email", user.getEmail() != null ? user.getEmail() : "");
                    map.put("name", user.getName() != null ? user.getName() : "");
                    map.put("profileImage", user.getProfileImage() != null ? user.getProfileImage() : "");
                    map.put("provider", user.getProvider() != null ? user.getProvider() : "");
                    map.put("role", user.getRole().name());
                    return map;
                })
                .toList();
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.status(404).body("User not found");
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(@RequestHeader("Authorization") String bearerToken) {
        if (!StringUtils.hasText(bearerToken) || !bearerToken.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        String token = bearerToken.substring(7);
        if (!jwtTokenProvider.validate(token)) {
            return ResponseEntity.status(401).body("Invalid token");
        }

        String email = jwtTokenProvider.getEmail(token);
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body("User not found");
        }

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "name", user.getName(),
                "nickname", user.getName() != null ? user.getName() : "",
                "profileImage", user.getProfileImage() != null ? user.getProfileImage() : "",
                "provider", user.getProvider()
        ));
    }
}