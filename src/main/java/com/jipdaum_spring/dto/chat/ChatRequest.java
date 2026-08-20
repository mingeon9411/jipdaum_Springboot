package com.jipdaum_spring.dto.chat;

import java.util.List;

/**
 * history는 선택 필드다 — 안 보내면 기존처럼 단발성 질문으로 처리된다.
 * 대화 상태는 서버 DB에 저장하지 않고 프론트가 들고 있다가 매 요청마다 통째로 보낸다
 * (Django가 스키마를 소유하고 있어 Spring 쪽에서 새 테이블을 함부로 못 만드는 제약 때문 — CLAUDE.md 참고).
 *
 * captchaToken은 현재 사용하지 않는다(hCaptcha 게이트 제거) — 이전 프론트와의 호환을 위해
 * 필드만 남겨두고 무시한다.
 *
 * channel은 어느 페이지에서 온 채팅인지 나타낸다("korean-hall" | null/그 외 = 기본 쇼핑몰).
 * 상품 검색 범위(collection)를 그 문맥에 맞게 고정하는 데 쓰인다(ChatService 참고).
 */
public record ChatRequest(String message, List<ChatMessage> history, String captchaToken, String channel) {
}
