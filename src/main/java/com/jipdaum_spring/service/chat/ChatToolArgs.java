package com.jipdaum_spring.service.chat;

/**
 * Gemini가 넘겨주는 function-call 인자(Map&lt;String,Object&gt;)의 값 타입은 JSON 파싱 결과라
 * Integer/Long/Double/String이 섞여 들어올 수 있다. 여러 ChatTool 구현체에서 반복되는 방어 캐스팅을 모아둔다.
 */
final class ChatToolArgs {

    private ChatToolArgs() {
    }

    static Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
