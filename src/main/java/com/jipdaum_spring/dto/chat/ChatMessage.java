package com.jipdaum_spring.dto.chat;

/**
 * 프론트가 유지하는 대화 이력의 한 턴.
 * role은 "user" 또는 "assistant"만 허용한다 (Gemini의 "model" role은 서버 내부에서만 사용).
 */
public record ChatMessage(String role, String content) {
}
