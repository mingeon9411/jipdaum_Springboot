package com.jipdaum_spring.controller;

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
import com.jipdaum_spring.service.UserAuthService;
import com.jipdaum_spring.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserAuthService userAuthService;
    private final UserService userService;

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
        return ResponseEntity.ok(userService.getAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (!userService.delete(id)) {
            return ResponseEntity.status(404).body("User not found");
        }
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe() {
        return ResponseEntity.ok(userService.getMe());
    }
}
