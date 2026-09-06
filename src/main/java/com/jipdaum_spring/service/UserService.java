package com.jipdaum_spring.service;

import com.jipdaum_spring.domain.jipdaumuser.JipdaumUser;
import com.jipdaum_spring.domain.springuser.User;
import com.jipdaum_spring.domain.springuser.UserRepository;
import com.jipdaum_spring.dto.user.MeResponse;
import com.jipdaum_spring.dto.user.UserResponse;
import com.jipdaum_spring.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * springuser.User(소셜 로그인 부가정보 테이블, `users`)에 대한 관리자용 조회/삭제와 내 정보 조회를 담당한다.
 * 회원가입/로그인/탈퇴 등 인증 자체(JIPDAUM_USER)는 UserAuthService 참고.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    public List<UserResponse> getAll() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    /** @return 삭제됐으면 true, 대상이 없어 아무 것도 하지 않았으면 false */
    @Transactional
    public boolean delete(Long id) {
        if (!userRepository.existsById(id)) {
            return false;
        }
        userRepository.deleteById(id);
        return true;
    }

    public MeResponse getMe() {
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

        return new MeResponse(
                jipdaumUser.getId(),
                jipdaumUser.getEmail(),
                jipdaumUser.getNickname(),
                jipdaumUser.getNickname(),
                springUser != null && springUser.getProfileImage() != null ? springUser.getProfileImage() : "",
                springUser != null ? springUser.getProvider() : "",
                jipdaumUser.getSecurityQuestion() != null,
                jipdaumUser.getSecurityQuestion()
        );
    }
}
