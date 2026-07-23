package com.jipdaum_spring.domain.token;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Spring 소유 테이블 {@code BLACKLISTED_TOKEN}을 없으면 생성한다.
 *
 * <p>로그아웃 시 refresh 토큰을 블랙리스트에 넣는 Spring 전용 테이블이다. Django 스키마가 아니라
 * {@code ddl-auto: none}이라 Hibernate도 만들지 않아 그동안 어느 환경에서도 생성되지 않았고, 로그아웃마다
 * {@code Table 'jibdaum.BLACKLISTED_TOKEN' doesn't exist}로 500이 났다. 시작 시 한 번 보장해준다.
 *
 * <p>Spring 소유 테이블만 만들 뿐 Django 테이블은 건드리지 않으므로 {@code ddl-auto: none} 원칙과 충돌하지 않는다.
 * 소셜 로그인용 users 테이블을 만드는 {@code SpringUserTableInitializer}와 동일한 패턴이다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BlacklistedTokenTableInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureBlacklistedTokenTable() {
        // 컬럼명은 BlacklistedToken 엔티티의 @Column 매핑(id, token, expires_at)과 정확히 일치해야 한다.
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS BLACKLISTED_TOKEN (" +
                "  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "  token VARCHAR(1000) NOT NULL," +
                "  expires_at DATETIME(6) NOT NULL" +
                ")"
            );
        } catch (DataAccessException e) {
            // 만들지 못해도 부팅은 막지 않는다.
            log.warn("BLACKLISTED_TOKEN 테이블 생성/확인 실패 — 로그아웃 시 토큰 블랙리스트 저장이 실패할 수 있음", e);
        }
    }
}
