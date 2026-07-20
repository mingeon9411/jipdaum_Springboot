package com.jipdaum_spring.exception;

import com.jipdaum_spring.dto.common.ErrorResponse;
import com.jipdaum_spring.security.jwt.TokenExpiredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

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