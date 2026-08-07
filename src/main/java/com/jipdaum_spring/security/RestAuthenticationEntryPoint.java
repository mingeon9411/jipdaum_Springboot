package com.jipdaum_spring.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * oauth2Login()이 설정돼 있으면 Spring Security가 인증 안 된 요청을 기본적으로
 * /login 페이지로 302 리다이렉트한다. 이 백엔드는 순수 REST API라 그 리다이렉트를
 * axios/fetch가 그대로 따라가 버리면 JSON을 기대하는 호출이 로그인 HTML 페이지를
 * 200 OK로 받아버려, 프론트에서 "배열이 아닌데 .filter를 호출" 같은 크래시로 이어진다
 * (예: 토큰 만료 후 마이페이지 진입). API는 항상 깔끔한 401 JSON을 받도록 강제한다.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\":401,\"message\":\"인증이 필요합니다.\"}");
    }
}
