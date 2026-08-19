package com.jipdaum_spring.dto.chat;

import java.util.List;

/**
 * history는 선택 필드다 — 안 보내면 기존처럼 단발성 질문으로 처리된다.
 * 대화 상태는 서버 DB에 저장하지 않고 프론트가 들고 있다가 매 요청마다 통째로 보낸다
 * (Django가 스키마를 소유하고 있어 Spring 쪽에서 새 테이블을 함부로 못 만드는 제약 때문 — CLAUDE.md 참고).
 *
 * captchaToken은 history가 비어있는 "새 대화 시작" 요청에서만 필수다 — 같은 대화의 후속 메시지까지
 * 매번 캡차를 풀게 하면 UX가 나빠지므로, 대화당 최초 1회만 사람인지 확인한다(ChatController 참고).
 */
public record ChatRequest(String message, List<ChatMessage> history, String captchaToken) {
}
