package com.jipdaum_spring.exception;

/**
 * 회원가입 폼 필드별 검증 실패. 프론트엔드 Register.jsx가 DRF 스타일로
 * err.response.data.email / .nickname / .password / .password_confirm / .non_field_errors를
 * 개별적으로 읽으므로, GlobalExceptionHandler가 {"<field>": message}로 직렬화한다.
 */
public class FieldValidationException extends RuntimeException {

    private final String field;

    public FieldValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
