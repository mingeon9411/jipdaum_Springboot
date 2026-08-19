package com.jipdaum_spring.security.oauth;

import com.jipdaum_spring.security.captcha.HCaptchaService;
import com.jipdaum_spring.security.captcha.SocialLoginCaptchaTicketStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 소셜 로그인(카카오/네이버/구글) 버튼도 봇이 반복 클릭해 OAuth provider에 부하를 주거나 계정을
 * 무차별 시도하는 데 악용될 수 있다. /oauth2/authorization/{id}는 브라우저가 직접 이동하는 GET
 * 요청이라 캡차 토큰을 JSON 바디로 못 보내므로, 프론트가 미리 POST /api/auth/social-captcha로
 * 검증받아 발급받은 1회용 ticket을 쿼리 파라미터로 요구한다.
 *
 * Spring Security의 OAuth2AuthorizationRequestRedirectFilter가 이 경로를 가로채 provider로
 * 리다이렉트해버리므로, 그보다 앞에 등록해야 한다(SecurityConfig 참고).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SocialLoginCaptchaFilter extends OncePerRequestFilter {

    private static final RequestMatcher MATCHER =
            new AntPathRequestMatcher("/oauth2/authorization/*");

    private final HCaptchaService hCaptchaService;
    private final SocialLoginCaptchaTicketStore socialLoginCaptchaTicketStore;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // secret key가 없는 로컬/테스트 환경에서는 캡차 자체가 꺼져 있다고 보고 그대로 통과시킨다
        // (HCaptchaService의 다른 모든 검증 지점과 동일한 정책).
        return !hCaptchaService.isEnabled() || !MATCHER.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String ticket = request.getParameter("ticket");
        if (socialLoginCaptchaTicketStore.consume(ticket)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("소셜 로그인 캡차 ticket 검증 실패: uri={}", request.getRequestURI());
        String message = "보안 인증이 필요합니다. 다시 시도해주세요.";
        String redirectUrl = frontendUrl + "/social-callback?error=" + UriUtils.encode(message, StandardCharsets.UTF_8);
        response.sendRedirect(redirectUrl);
    }
}
