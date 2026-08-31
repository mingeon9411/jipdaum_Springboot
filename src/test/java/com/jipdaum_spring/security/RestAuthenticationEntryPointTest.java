package com.jipdaum_spring.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SecurityConfig가 이 엔트리포인트를 /api/**에 등록해야 미인증 요청이 302(/login) 대신 401
 * JSON을 받는다(2b2d579 — 302 리다이렉트 케이스 스터디 참고). 여기서는 SecurityConfig 와이어링
 * 없이, 엔트리포인트 자체가 실제로 401 JSON을 쓰는지만 고정한다.
 */
class RestAuthenticationEntryPointTest {

    private final RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint();

    @Test
    void 미인증_요청은_302가_아니라_401_JSON을_받는다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("no auth"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getRedirectedUrl()).isNull();
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).contains("인증이 필요합니다");
    }
}
