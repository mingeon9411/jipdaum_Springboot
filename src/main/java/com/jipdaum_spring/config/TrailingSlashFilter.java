package com.jipdaum_spring.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * React(Django 방식)에서 전송되는 trailing slash URL을 Spring Boot가 처리할 수 있도록
 * 요청 URI 끝의 슬래시를 제거하는 필터.
 * 예: /api/shop/cart/  →  /api/shop/cart
 */
@Component
public class TrailingSlashFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri.length() > 1 && uri.endsWith("/")) {
            String trimmed = uri.substring(0, uri.length() - 1);
            String query = request.getQueryString();
            HttpServletRequest wrapped = new HttpServletRequestWrapper(request) {
                @Override public String getRequestURI() { return trimmed; }
                @Override public StringBuffer getRequestURL() {
                    StringBuffer url = new StringBuffer(request.getScheme())
                            .append("://").append(request.getServerName());
                    if (request.getServerPort() != 80 && request.getServerPort() != 443)
                        url.append(":").append(request.getServerPort());
                    url.append(trimmed);
                    return url;
                }
            };
            chain.doFilter(wrapped, response);
            return;
        }
        chain.doFilter(request, response);
    }
}
