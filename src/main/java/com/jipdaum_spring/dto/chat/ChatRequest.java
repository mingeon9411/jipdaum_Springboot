package com.jipdaum_spring.dto.chat;

import java.util.List;

/**
 * history는 선택 필드다 — 안 보내면 기존처럼 단발성 질문으로 처리된다.
 * 대화 상태는 서버 DB에 저장하지 않고 프론트가 들고 있다가 매 요청마다 통째로 보낸다
 * (Django가 스키마를 소유하고 있어 Spring 쪽에서 새 테이블을 함부로 못 만드는 제약 때문 — CLAUDE.md 참고).
 */
public record ChatRequest(String message, List<ChatMessage> history) {
}
