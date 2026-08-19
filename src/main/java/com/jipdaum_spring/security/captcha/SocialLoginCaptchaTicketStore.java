package com.jipdaum_spring.security.captcha;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 소셜 로그인 버튼 클릭 → /oauth2/authorization/{id} 리다이렉트는 브라우저의 일반 페이지 이동이라
 * JSON 요청 바디에 캡차 토큰을 실어 보낼 수 없다. 그래서 프론트가 먼저 POST /api/auth/social-captcha
 * 로 hCaptcha 토큰을 검증받아 1회용 ticket을 받고, 그 ticket을 쿼리 파라미터로 붙여
 * /oauth2/authorization/{id}?ticket=... 을 호출하는 2단계로 우회한다.
 *
 * SocialLoginCodeStore와 동일한 패턴(짧은 TTL, 조회 즉시 소비)이다.
 */
@Component
public class SocialLoginCaptchaTicketStore {

    private static final long TTL_MILLIS = 60_000L;

    private record Entry(long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    private final Map<String, Entry> tickets = new ConcurrentHashMap<>();

    public String issue() {
        tickets.values().removeIf(Entry::isExpired);
        String ticket = UUID.randomUUID().toString();
        tickets.put(ticket, new Entry(System.currentTimeMillis() + TTL_MILLIS));
        return ticket;
    }

    /** ticket을 소비한다. 존재하지 않거나 만료됐으면(이미 쓴 경우 포함) false. */
    public boolean consume(String ticket) {
        if (ticket == null) {
            return false;
        }
        Entry entry = tickets.remove(ticket);
        return entry != null && !entry.isExpired();
    }
}
