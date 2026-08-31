package com.jipdaum_spring.exception;

import com.jipdaum_spring.dto.common.ErrorResponse;
import com.jipdaum_spring.security.jwt.TokenExpiredException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 이 핸들러들 중 하나라도 비면, 그 예외는 Boot 기본 /error 포워드 -> SecurityConfig의
 * anyRequest().authenticated()에 걸려 axios가 302(로그인 HTML)를 그대로 따라가다 크래시난다
 * (2b2d579, 90b0ef2 — #Developer_Document의 302 리다이렉트 케이스 스터디 참고). 여기서는 실제
 * HTTP/Security 스택 없이, "이 예외가 들어오면 이 핸들러가 몇 상태코드로 무슨 모양을 반환하는가"만
 * 고정한다 — 새 핸들러 우선순위를 잘못 추가해도 여기서 바로 깨지게 하는 게 목적.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void Valid_검증_실패는_필드별_메시지_맵으로_400을_반환한다() {
        MethodArgumentNotValidException e = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(e.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("register", "email", "이메일 형식이 아닙니다")));

        ResponseEntity<?> response = handler.handleMethodArgumentNotValid(e);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(Map.of("email", "이메일 형식이 아닙니다"));
    }

    @Test
    void 처리되지_않은_예외는_원인을_노출하지_않고_500_JSON을_반환한다() {
        ResponseEntity<?> response = handler.handleUnexpected(new NullPointerException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
        assertThat(((ErrorResponse) response.getBody()).error()).doesNotContain("boom");
    }

    @Test
    void ResponseStatusException은_같은_상태코드와_사유로_응답한다() {
        ResponseStatusException e = new ResponseStatusException(HttpStatus.CONFLICT, "이미 처리된 주문입니다");

        ResponseEntity<?> response = handler.handleResponseStatusException(e);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo(Map.of("status", 409, "message", "이미 처리된 주문입니다"));
    }

    @Test
    void FieldValidationException은_field_message_형태로_400을_반환한다() {
        ResponseEntity<?> response = handler.handleFieldValidation(new FieldValidationException("nickname", "이미 사용 중입니다"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(Map.of("nickname", "이미 사용 중입니다"));
    }

    @Test
    void AuthException은_지정된_상태코드로_error_메시지를_반환한다() {
        ResponseEntity<?> response = handler.handleAuthException(new AuthException(HttpStatus.UNAUTHORIZED, "비밀번호가 틀렸습니다"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo(new ErrorResponse("비밀번호가 틀렸습니다"));
    }

    @Test
    void TokenExpiredException은_TOKEN_EXPIRED_코드와_함께_401을_반환한다() {
        ResponseEntity<?> response = handler.handleTokenExpired(new TokenExpiredException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo(Map.of("code", "TOKEN_EXPIRED", "message", "액세스 토큰이 만료되었습니다."));
    }
}
