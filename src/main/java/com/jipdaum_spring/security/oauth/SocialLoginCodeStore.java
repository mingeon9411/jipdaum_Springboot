package com.jipdaum_spring.security.oauth;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OAuth2 로그인 성공 시 access/refresh 토큰을 리다이렉트 URL에 직접 노출하지 않기 위한
 * 1회용 교환 코드 저장소. 코드는 짧은 TTL 동안만 유효하고, 조회 즉시 제거되어 재사용할 수 없다.
 */
@Component
public class SocialLoginCodeStore {

    private static final long TTL_MILLIS = 60_000L;

    private record Entry(String email, long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    private final Map<String, Entry> codes = new ConcurrentHashMap<>();

    public String issue(String email) {
        codes.values().removeIf(Entry::isExpired);
        String code = UUID.randomUUID().toString();
        codes.put(code, new Entry(email, System.currentTimeMillis() + TTL_MILLIS));
        return code;
    }

    /** 코드를 소비해 이메일을 반환한다. 존재하지 않거나 만료됐으면 즉시 제거하고 null을 반환한다. */
    public String consume(String code) {
        Entry entry = codes.remove(code);
        if (entry == null || entry.isExpired()) {
            return null;
        }
        return entry.email();
    }
}