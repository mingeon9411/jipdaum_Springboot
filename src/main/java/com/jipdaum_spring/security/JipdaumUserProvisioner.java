package com.jipdaum_spring.security;

import com.jipdaum_spring.domain.jipdaumuser.JipdaumUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Django와 공유하는 JIPDAUM_USER 테이블에 Spring 로그인 사용자가 없으면 채워 넣는다.
 * Order/Cart/Review 등 다수의 FK가 이 테이블을 참조하기 때문에, 소셜 로그인(CustomOAuth2UserService)과
 * 기존 세션 토큰 호환 경로(JwtAuthenticationFilter) 양쪽에서 동일하게 필요해 여기로 모았다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JipdaumUserProvisioner {

    private final JipdaumUserRepository jipdaumUserRepository;
    private final JdbcTemplate jdbcTemplate;

    /** email로 JIPDAUM_USER 존재를 보장한다. 이미 있으면(email 또는 username 대소문자 무시 일치) 아무것도 하지 않는다. */
    @Transactional
    public void ensureExists(String email, String usernameHint, String nickname) {
        if (jipdaumUserRepository.findByEmail(email).isPresent()
                || !jipdaumUserRepository.findAllByUsernameIgnoreCase(email).isEmpty()) {
            return;
        }

        try {
            insert(usernameHint, email, nickname);
        } catch (DataAccessException e) {
            // username unique 제약 충돌 시 suffix를 붙여 한 번 더 시도한다.
            String fallbackUsername = usernameHint + "_" + (System.currentTimeMillis() % 10000);
            try {
                insert(fallbackUsername, email, nickname);
            } catch (DataAccessException retryFailure) {
                log.warn("JIPDAUM_USER 자동 생성 실패 (email={})", email, retryFailure);
            }
        }
    }

    private void insert(String username, String email, String nickname) {
        jdbcTemplate.update(
            "INSERT INTO JIPDAUM_USER " +
            "(username, password, last_login, is_superuser, first_name, last_name, " +
            "email, is_staff, is_active, created_at, nickname, is_email_verified) " +
            "VALUES (?, ?, NULL, 0, '', '', ?, 0, 1, SYSDATE, ?, 0)",
            username, "!" + username, email, nickname
        );
    }
}