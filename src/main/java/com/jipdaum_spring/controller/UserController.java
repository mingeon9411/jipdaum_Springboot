package com.jipdaum_spring.controller;

import com.jipdaum_spring.domain.jipdaumuser.JipdaumUser;
import com.jipdaum_spring.domain.springuser.User;
import com.jipdaum_spring.domain.springuser.UserRepository;
import com.jipdaum_spring.dto.auth.EmailOtpSendRequest;
import com.jipdaum_spring.dto.auth.EmailOtpVerifyRequest;
import com.jipdaum_spring.dto.auth.LoginRequest;
import com.jipdaum_spring.dto.auth.LoginResponse;
import com.jipdaum_spring.dto.auth.LogoutRequest;
import com.jipdaum_spring.dto.auth.NicknameCheckResponse;
import com.jipdaum_spring.dto.auth.RegisterRequest;
import com.jipdaum_spring.dto.auth.RegisterResponse;
import com.jipdaum_spring.dto.auth.WithdrawRequest;
import com.jipdaum_spring.dto.common.MessageResponse;
import com.jipdaum_spring.security.CurrentUserProvider;
import com.jipdaum_spring.service.UserAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserAuthService userAuthService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = userAuthService.register(request);
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userAuthService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody LogoutRequest request) {
        userAuthService.logout(request);
        return ResponseEntity.ok(new MessageResponse("로그아웃 완료"));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@RequestBody(required = false) WithdrawRequest request) {
        userAuthService.withdraw(request != null ? request : new WithdrawRequest(null));
        return ResponseEntity.ok(new MessageResponse("탈퇴가 완료되었습니다."));
    }

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
    public ResponseEntity<?> getMe() {
        // CurrentUserProvider를 통해야 JwtAuthenticationFilter가 강제하는 만료/탈퇴(is_active=0) 체크가
        // 그대로 적용된다 — 직접 토큰을 파싱하면 이 체크들을 우회하게 되므로 반드시 이 경로를 거친다.
        JipdaumUser jipdaumUser = currentUserProvider.getCurrentUser();

        // JIPDAUM_USER(Django와 공유하는 테이블)가 실제 nickname의 source of truth.
        // Spring 자체 users 테이블은 소셜 로그인 부가 정보(프로필 이미지 등)만 보조로 사용한다.
        User springUser;
        try {
            springUser = userRepository.findByEmail(jipdaumUser.getEmail()).orElse(null);
        } catch (DataAccessException e) {
            log.warn("users 테이블 조회 실패 — 부가 정보 없이 응답 (email={})", jipdaumUser.getEmail(), e);
            springUser = null;
        }

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