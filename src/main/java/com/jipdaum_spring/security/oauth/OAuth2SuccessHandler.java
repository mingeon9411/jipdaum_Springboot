package com.jipdaum_spring.security.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final SocialLoginCodeStore socialLoginCodeStore;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getEmail();

        // access/refresh 토큰을 URL에 직접 실어 보내지 않고, 짧게 사는 1회용 코드로 교환한다.
        // 프론트엔드는 이 code를 POST /api/auth/social-exchange 로 보내 실제 토큰을 받아야 한다.
        String code = socialLoginCodeStore.issue(email);

        String redirectUrl = frontendUrl + "/social-callback?code=" + code;
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}