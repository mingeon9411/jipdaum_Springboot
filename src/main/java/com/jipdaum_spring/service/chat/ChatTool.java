package com.jipdaum_spring.service.chat;

import java.util.Map;

/**
 * Gemini function-calling에 노출할 도구. Gemini가 name/description/parameters를 보고
 * 스스로 호출 여부와 인자를 판단하며, 우리는 실행 결과만 돌려주면 된다.
 * parameters()는 Gemini REST의 OpenAPI 서브셋 스키마(type: "OBJECT"/"STRING"/"INTEGER" 등)를 따른다.
 */
public interface ChatTool {
    String name();

    String description();

    Map<String, Object> parameters();

    /**
     * args의 값 타입은 Gemini가 JSON으로 보낸 그대로(Number/String 등)라 구현체에서 방어적으로 캐스팅해야 한다.
     * 예외를 던지면 GeminiClient가 잡아서 실패 결과를 모델에게 돌려준다.
     */
    Object execute(Map<String, Object> args);
}
