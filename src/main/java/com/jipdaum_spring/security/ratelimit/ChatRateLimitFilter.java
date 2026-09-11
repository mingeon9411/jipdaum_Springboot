package com.jipdaum_spring.security.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jipdaum_spring.security.CustomUserDetails;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * LLM(Gemini) 호출 비용이 드는 챗봇 엔드포인트({@code POST /api/shop/chat})를 대상으로 한 rate limit.
 * 로그인 여부와 무관하게 permitAll로 열려 있는 엔드포인트라, 로그인 사용자는 계정 단위로,
 * 비로그인 방문자는 IP 단위로 버킷을 나눠 과도한 반복 호출(비용 남용/DoS)을 막는다.
 *
 * 버킷은 Caffeine 캐시에 보관해 접근이 없으면 자동 만료시킨다(키를 계속 바꿔가며 요청하는
 * 방식으로 버킷 자체가 무한히 쌓여 메모리를 고갈시키는 것을 방지하기 위함).
 */
@Slf4j
@Component
public class ChatRateLimitFilter extends OncePerRequestFilter {

    private static final String CHAT_PATH = "/api/shop/chat";
    private static final String SEMANTIC_SEARCH_PATH = "/api/shop/products/semantic-search";

    @Value("${app.chat.rate-limit.capacity:10}")
    private int capacity;

    @Value("${app.chat.rate-limit.refill-tokens:10}")
    private int refillTokens;

    @Value("${app.chat.rate-limit.refill-minutes:1}")
    private long refillMinutes;

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(50_000)
            .expireAfterAccess(Duration.ofMinutes(30))
            .build();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !(HttpMethod.POST.matches(request.getMethod()) && CHAT_PATH.equals(request.getRequestURI()))
                && !(HttpMethod.GET.matches(request.getMethod()) && SEMANTIC_SEARCH_PATH.equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String key = resolveKey(request);
        Bucket bucket = buckets.get(key, k -> newBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("챗봇 rate limit 초과: key={}", key);
        response.setStatus(429); // Too Many Requests (jakarta.servlet에는 상수가 없음)
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"요청이 너무 많습니다. 잠시 후 다시 시도해주세요.\"}");
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(limit -> limit.capacity(capacity).refillGreedy(refillTokens, Duration.ofMinutes(refillMinutes)))
                .build();
    }

    /** 로그인 사용자는 이메일(계정) 단위, 비로그인 방문자는 IP 단위로 버킷 키를 나눈다. */
    private String resolveKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            return "user:" + userDetails.getUsername();
        }
        return "ip:" + resolveClientIp(request);
    }

    /**
     * 리버스 프록시(예: EC2 앞단 nginx/ALB) 뒤에서 실행되는 경우를 대비해 X-Forwarded-For를
     * 우선 사용한다. 헤더는 클라이언트가 임의로 조작 가능하지만, 프록시가 없는 로컬/직접 접속
     * 환경에서도 최소한 getRemoteAddr()로는 동작해야 하므로 폴백을 둔다.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
