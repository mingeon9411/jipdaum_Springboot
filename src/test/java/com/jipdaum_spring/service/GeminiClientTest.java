package com.jipdaum_spring.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * GeminiClient.generate()의 함수 호출 라운드 루프를 고정한다 — 실제 Gemini 호출 없이
 * MockRestServiceServer로 응답만 흉내낸다. 1fe06e5(챗봇 응답 지연 개선 케이스 스터디)가
 * 코드로만 보장하고 테스트로는 고정하지 않았던 두 가지: "functionCall이 없으면 1라운드에서
 * 바로 끝난다"와 "MAX_TOOL_CALL_ROUNDS를 넘기면 예외 없이 폴백(empty)한다".
 */
class GeminiClientTest {

    private static final String GENERATE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.7-flash:generateContent";

    private GeminiClient newClientBoundTo(MockRestServiceServer[] serverOut) {
        GeminiClient client = new GeminiClient();
        ReflectionTestUtils.setField(client, "apiKey", "test-key");
        ReflectionTestUtils.setField(client, "model", "gemini-3.7-flash");
        ReflectionTestUtils.setField(client, "baseUrl", "https://generativelanguage.googleapis.com/v1beta");

        RestClient.Builder builder = RestClient.builder();
        serverOut[0] = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(client, "restClient", builder.build());
        return client;
    }

    @Test
    void functionCall_없이_텍스트만_오면_1라운드에서_끝난다() {
        MockRestServiceServer[] serverOut = new MockRestServiceServer[1];
        GeminiClient client = newClientBoundTo(serverOut);
        serverOut[0].expect(requestTo(GENERATE_URL))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"안녕하세요, 무엇을 도와드릴까요?"}]}}]}
                        """, MediaType.APPLICATION_JSON));

        Optional<GeminiClient.ChatReply> reply = client.generate("system", List.of(), "안녕", List.of());

        assertThat(reply).isPresent();
        assertThat(reply.get().text()).isEqualTo("안녕하세요, 무엇을 도와드릴까요?");
        serverOut[0].verify(); // 정확히 1번만 호출됐는지(추가 라운드가 안 돌았는지) 확인
    }

    @Test
    void functionCall이_라운드_상한을_넘기면_예외_없이_폴백한다() {
        MockRestServiceServer[] serverOut = new MockRestServiceServer[1];
        GeminiClient client = newClientBoundTo(serverOut);
        String functionCallResponse = """
                {"candidates":[{"content":{"parts":[{"functionCall":{"name":"search_products","args":{}}}]}}]}
                """;
        // MAX_TOOL_CALL_ROUNDS(3)만큼 매번 functionCall만 반환 — 텍스트가 끝내 안 나오는 최악의 경우.
        serverOut[0].expect(requestTo(GENERATE_URL)).andRespond(withSuccess(functionCallResponse, MediaType.APPLICATION_JSON));
        serverOut[0].expect(requestTo(GENERATE_URL)).andRespond(withSuccess(functionCallResponse, MediaType.APPLICATION_JSON));
        serverOut[0].expect(requestTo(GENERATE_URL)).andRespond(withSuccess(functionCallResponse, MediaType.APPLICATION_JSON));

        Optional<GeminiClient.ChatReply> reply = client.generate("system", List.of(), "소파 추천해줘", List.of());

        assertThat(reply).isEmpty();
        serverOut[0].verify(); // 정확히 3번(상한)만 호출되고 4번째는 없었는지 확인
    }
}
