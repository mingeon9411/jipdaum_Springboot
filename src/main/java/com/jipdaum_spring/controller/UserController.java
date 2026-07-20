package com.jipdaum_spring.controller;

import com.jipdaum_spring.domain.jipdaumuser.JipdaumUser;
import com.jipdaum_spring.domain.jipdaumuser.JipdaumUserRepository;
import com.jipdaum_spring.domain.springuser.User;
import com.jipdaum_spring.domain.springuser.UserRepository;
import com.jipdaum_spring.dto.auth.EmailOtpSendRequest;
import com.jipdaum_spring.dto.auth.EmailOtpVerifyRequest;
import com.jipdaum_spring.dto.auth.NicknameCheckResponse;
import com.jipdaum_spring.dto.common.MessageResponse;
import com.jipdaum_spring.security.jwt.JwtTokenProvider;
import com.jipdaum_spring.service.UserAuthService;
import jakarta.validation.Valid;
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
    private final JipdaumUserRepository jipdaumUserRepository;
    private final UserAuthService userAuthService;

    @GetMapping("/nickname-check")
    public ResponseEntity<?> checkNickname(@RequestParam String nickname) {
        NicknameCheckResponse response = userAuthService.checkNickname(nickname);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/email-verify/send")
    public ResponseEntity<?> sendEmailOtp(@Valid @RequestBody EmailOtpSendRequest request) {
        userAuthService.sendEmailOtp(request);
        return ResponseEntity.ok(new MessageResponse("인증 코드가 발송되었습니다."));
    }

    @PostMapping("/email-verify/confirm")
    public ResponseEntity<?> verifyEmailOtp(@Valid @RequestBody EmailOtpVerifyRequest request) {
        userAuthService.verifyEmailOtp(request);
        return ResponseEntity.ok(new MessageResponse("이메일 인증이 완료되었습니다."));
    }

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
        if (JwtTokenProvider.TYPE_REFRESH.equals(jwtTokenProvider.getType(token))) {
            return ResponseEntity.status(401).body(Map.of("code", "INVALID_TOKEN", "message", "Invalid token"));
        }
        JwtTokenProvider.TokenStatus status = jwtTokenProvider.validateToken(token);
        if (status == JwtTokenProvider.TokenStatus.EXPIRED) {
            return ResponseEntity.status(401).body(Map.of("code", "TOKEN_EXPIRED", "message", "액세스 토큰이 만료되었습니다."));
        }
        if (status != JwtTokenProvider.TokenStatus.VALID) {
            return ResponseEntity.status(401).body(Map.of("code", "INVALID_TOKEN", "message", "Invalid token"));
        }

        String email = jwtTokenProvider.getEmail(token);

        // Django SimpleJWT: sub 없이 user_id 클레임 사용
        if (email == null) {
            Long userId = jwtTokenProvider.getUserId(token);
            if (userId != null) {
                email = jipdaumUserRepository.findById(userId).map(JipdaumUser::getEmail).orElse(null);
            }
        }
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("code", "INVALID_TOKEN", "message", "Invalid token"));
        }

        // JIPDAUM_USER(Django와 공유하는 테이블)가 실제 nickname의 source of truth.
        // Spring 자체 users 테이블은 소셜 로그인 부가 정보(프로필 이미지 등)만 보조로 사용한다.
        JipdaumUser jipdaumUser = jipdaumUserRepository.findByEmail(email).orElse(null);
        if (jipdaumUser == null) {
            return ResponseEntity.status(404).body("User not found");
        }
        User springUser = userRepository.findByEmail(email).orElse(null);

        Map<String, Object> body = new HashMap<>();
        body.put("id", jipdaumUser.getId());
        body.put("email", jipdaumUser.getEmail());
        body.put("name", jipdaumUser.getNickname());
        body.put("nickname", jipdaumUser.getNickname());
        body.put("profileImage", springUser != null && springUser.getProfileImage() != null ? springUser.getProfileImage() : "");
        body.put("provider", springUser != null ? springUser.getProvider() : "");
        return ResponseEntity.ok(body);
    }
}