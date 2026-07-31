package com.jipdaum_spring.security.oauth;

import com.jipdaum_spring.domain.jipdaumuser.JipdaumUserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final SocialLoginCodeStore socialLoginCodeStore;
    private final JipdaumUserRepository jipdaumUserRepository;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getEmail();

        // 탈퇴(is_active=0)한 계정은 JwtAuthenticationFilter가 토큰을 발급받아도 인증을 걸어주지
        // 않아 마이페이지 등 모든 API가 조용히 401을 반환한다 — 여기서 미리 걸러서 프론트엔드가
        // 원인을 바로 안내할 수 있게 한다.
        boolean withdrawn = jipdaumUserRepository.findByEmail(email)
                .map(u -> Boolean.FALSE.equals(u.getActive()))
                .orElse(false);
        if (withdrawn) {
            String redirectUrl = frontendUrl + "/social-callback?error=" +
                    URLEncoder.encode("withdrawn", StandardCharsets.UTF_8);
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
            return;
        }

        // access/refresh 토큰을 URL에 직접 실어 보내지 않고, 짧게 사는 1회용 코드로 교환한다.
        // 프론트엔드는 이 code를 POST /api/auth/social-exchange 로 보내 실제 토큰을 받아야 한다.
        String code = socialLoginCodeStore.issue(email);

        String redirectUrl = frontendUrl + "/social-callback?code=" + code;
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}