package com.jipdaum_spring.security.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * oauth2Login()에 successHandler만 있고 failureHandler가 없으면, 콜백 처리 중 예외(예:
 * CustomOAuth2UserService의 NPE)가 나도 조용히 Spring 기본 /login 페이지로 되돌아가서
 * 원인을 알 수 없다. 실패 사유를 로그로 남기고 프론트에도 넘겨서 디버깅 가능하게 한다.
 */
@Slf4j
@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {

        log.warn("소셜 로그인 실패", exception);

        String message = exception.getMessage() != null ? exception.getMessage() : "소셜 로그인에 실패했습니다.";
        String encodedMessage = UriUtils.encode(message, StandardCharsets.UTF_8);
        String redirectUrl = frontendUrl + "/social-callback?error=" + encodedMessage;
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}