package com.jipdaum_spring.domain.springuser;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Spring 소유 보조 테이블 {@code users}를 없으면 생성한다.
 *
 * <p>실제 사용자/FK 기준 테이블은 Django와 공유하는 {@code JIPDAUM_USER}이고, {@code users}는 소셜 로그인의
 * 부가 정보(provider, providerId, profileImage)를 담는 Spring 전용 테이블이다. Django 스키마가 아니므로
 * Django 마이그레이션이 만들지 않고, {@code ddl-auto: none}이라 Hibernate도 만들지 않아 그동안 어느
 * 환경에서도 생성되지 않았다({@code Table 'jibdaum.users' doesn't exist}). 여기서 시작 시 한 번 보장해준다.
 *
 * <p>Spring 소유 테이블만 {@code CREATE TABLE IF NOT EXISTS}로 만들 뿐 Django 테이블은 건드리지 않으므로
 * {@code ddl-auto: none}으로 Django 스키마를 보호하는 원칙과 충돌하지 않는다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SpringUserTableInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureUsersTable() {
        // 컬럼명은 Hibernate가 조회하는 이름과 정확히 일치해야 한다.
        // application.yml의 PhysicalNamingStrategyStandardImpl은 camelCase를 그대로 유지하므로
        // profileImage / providerId도 스네이크로 바꾸지 않고 그대로 둔다.
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS users (" +
                "  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "  email VARCHAR(255)," +
                "  name VARCHAR(255)," +
                "  profileImage VARCHAR(512)," +
                "  provider VARCHAR(50)," +
                "  providerId VARCHAR(255)," +
                "  role VARCHAR(20)" +
                ")"
            );
        } catch (DataAccessException e) {
            // 만들지 못해도 부팅은 막지 않는다 — users는 보조 테이블이고 모든 사용처가 조회/저장 실패를 감내한다.
            log.warn("users 테이블 생성/확인 실패 — 소셜 프로필 부가 정보 저장이 비활성화된 채로 계속 진행", e);
        }
    }
}