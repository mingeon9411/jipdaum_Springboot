package com.jipdaum_spring.exception;

import com.jipdaum_spring.dto.common.ErrorResponse;
import com.jipdaum_spring.security.jwt.TokenExpiredException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<?> handleTokenExpired(TokenExpiredException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("code", "TOKEN_EXPIRED", "message", "액세스 토큰이 만료되었습니다."));
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<?> handleAuthException(AuthException e) {
        return ResponseEntity.status(e.getStatus()).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(FieldValidationException.class)
    public ResponseEntity<?> handleFieldValidation(FieldValidationException e) {
        return ResponseEntity.badRequest().body(Map.of(e.getField(), e.getMessage()));
    }

    /**
     * @Valid 실패(MethodArgumentNotValidException)는 여기 없으면 Boot 기본 /error로 빠져
     * ResponseStatusException과 같은 302 리다이렉트 문제를 겪는다. FieldValidationException과
     * 같은 {"<field>": message} 모양으로 맞춰야 프론트(Register.jsx 등)의 필드별 에러 표시가 동작한다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            body.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * 위에서 못 잡은 예외(NPE 등)를 그냥 두면 Boot의 /error 포워드 -> Security의
     * anyRequest().authenticated()에 걸려 axios가 302를 따라가다 크래시난다(위 주석과 동일 원인).
     * 스택트레이스는 서버 로그로만 남기고 클라이언트에는 원인을 노출하지 않는다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnexpected(Exception e) {
        log.error("처리되지 않은 예외", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("서버 오류가 발생했습니다."));
    }

    /**
     * ResponseStatusException을 그대로 두면 서블릿 response.sendError()를 거쳐
     * Boot의 /error 포워드 -> Spring Security의 anyRequest().authenticated() 규칙에 걸려
     * "/login"으로 302 리다이렉트되어버린다 (permitAll 경로에서도 발생).
     * 여기서 직접 JSON 응답을 만들어 그 경로를 우회한다.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatusException(ResponseStatusException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", e.getStatusCode().value());
        body.put("message", e.getReason());
        return ResponseEntity.status(e.getStatusCode()).body(body);
    }
}