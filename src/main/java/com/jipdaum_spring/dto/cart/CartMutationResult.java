package com.jipdaum_spring.dto.cart;

/** 컨트롤러가 201(생성)/200(갱신)을 메시지 문자열 매칭 없이 결정할 수 있도록 하는 서비스 내부 반환값. */
public record CartMutationResult(String message, boolean created) {
}