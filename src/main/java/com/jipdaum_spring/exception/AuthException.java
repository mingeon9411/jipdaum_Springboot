package com.jipdaum_spring.exception;

import org.springframework.http.HttpStatus;

/**
 * 프론트엔드(Team-DaumAna/frontend)가 err.response.data.error 형태로 읽는
 * 인증 관련 에러 전용 예외. GlobalExceptionHandler가 {"error": message}로 직렬화한다.
 */
public class AuthException extends RuntimeException {

    private final HttpStatus status;

    public AuthException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
