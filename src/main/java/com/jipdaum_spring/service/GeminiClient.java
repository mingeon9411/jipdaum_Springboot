package com.jipdaum_spring.service;

import com.jipdaum_spring.dto.chat.ChatMessage;
import com.jipdaum_spring.service.chat.ChatTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Google AI Studio의 Gemini generateContent REST API를 호출한다.
 * HCaptchaVerifier와 동일한 정책: api-key가 비어있는 환경(로컬 미설정)에서는 호출 자체를 건너뛰고,
 * 호출이 실패하면 예외를 던지지 않고 안전하게 empty를 반환한다 — 호출부(ChatService)가 규칙 기반
 * 답변으로 폴백할 수 있도록.
 *
 * tools가 주어지면 Gemini function-calling 루프를 돈다: 모델이 functionCall을 요청하면 로컬에서
 * 해당 ChatTool을 실행하고 결과를 다시 넣어 재호출 — 최종 텍스트가 나올 때까지 반복한다.
 * (실제 API로 검증한 요구사항: functionResponse는 role "user"로 보내야 하고, 모델이 반환한
 * functionCall 파트의 thoughtSignature를 그대로 echo하지 않으면 400 에러가 난다.)
 */
@Slf4j
@Component
public class GeminiClient {

    private static final int MAX_TOOL_CALL_ROUNDS = 4;

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-3.7-flash}")
    private String model;

    @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String baseUrl;

    private final WebClient webClient = WebClient.create();

    public Optional<String> generate(String systemPrompt, List<ChatMessage> history, String userMessage,
                                      List<ChatTool> tools) {
        if (!StringUtils.hasText(apiKey)) {
            log.warn("gemini.api-key가 설정되지 않아 LLM 호출을 건너뜁니다.");
            return Optional.empty();
        }

        List<Map<String, Object>> contents = new ArrayList<>();
        for (ChatMessage turn : history) {
            contents.add(toContent(mapRole(turn.role()), List.of(Map.of("text", turn.content()))));
        }
        contents.add(toContent("user", List.of(Map.of("text", userMessage))));

        Map<String, ChatTool> toolsByName = tools.stream()
                .collect(Collectors.toMap(ChatTool::name, t -> t));
        List<Map<String, Object>> toolDeclarations = tools.isEmpty()
                ? null
                : List.of(Map.of("functionDeclarations", tools.stream().map(this::toFunctionDeclaration).toList()));

        try {
            for (int round = 0; round < MAX_TOOL_CALL_ROUNDS; round++) {
                Map<?, ?> response = call(systemPrompt, contents, toolDeclarations);
                Map<?, ?> content = extractContent(response);
                if (content == null) {
                    return Optional.empty();
                }
                List<?> parts = (List<?>) content.get("parts");
                if (parts == null || parts.isEmpty()) {
                    return Optional.empty();
                }

                List<Map<?, ?>> functionCalls = parts.stream()
                        .<Map<?, ?>>map(p -> (Map<?, ?>) p)
                        .filter(p -> p.get("functionCall") != null)
                        .toList();

                if (functionCalls.isEmpty()) {
                    String text = parts.stream()
                            .map(p -> (Map<?, ?>) p)
                            .filter(p -> p.get("text") != null)
                            .map(p -> p.get("text").toString())
                            .collect(Collectors.joining());
                    return StringUtils.hasText(text) ? Optional.of(text) : Optional.empty();
                }

                // 모델의 functionCall 턴(thoughtSignature 포함)을 그대로 히스토리에 echo해야 다음 호출이 유효하다.
                contents.add(toContent("model", parts));
                contents.add(toContent("user", executeToolCalls(functionCalls, toolsByName)));
            }
        } catch (Exception e) {
            log.warn("Gemini API 호출 실패", e);
            return Optional.empty();
        }

        log.warn("도구 호출 라운드 한도({})를 초과해 응답을 생성하지 못했습니다.", MAX_TOOL_CALL_ROUNDS);
        return Optional.empty();
    }

    private Map<?, ?> call(String systemPrompt, List<Map<String, Object>> contents,
                            List<Map<String, Object>> toolDeclarations) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", contents);
        body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))));
        if (toolDeclarations != null) {
            body.put("tools", toolDeclarations);
        }

        return webClient.post()
                .uri(baseUrl + "/models/" + model + ":generateContent?key=" + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                // Gemini가 트래픽 급증 시 503(UNAVAILABLE)을 종종 반환한다 — 짧게 2회만 재시도.
                .retryWhen(Retry.backoff(1, Duration.ofMillis(500)).filter(this::isRetryable))
                // 정상 응답은 보통 2~4초 안에 온다. 실패 시 사용자를 오래 기다리게 하지 않고
                // 빨리 규칙 기반 폴백으로 넘어가는 게 30초 만석 대기보다 훨씬 나은 UX다.
                .block(Duration.ofSeconds(12));
    }

    private List<Map<String, Object>> executeToolCalls(List<Map<?, ?>> functionCalls, Map<String, ChatTool> toolsByName) {
        List<Map<String, Object>> responseParts = new ArrayList<>();
        for (Map<?, ?> callPart : functionCalls) {
            Map<?, ?> functionCall = (Map<?, ?>) callPart.get("functionCall");
            String name = String.valueOf(functionCall.get("name"));
            Object rawArgs = functionCall.get("args");
            @SuppressWarnings("unchecked")
            Map<String, Object> args = rawArgs != null ? (Map<String, Object>) rawArgs : Map.of();

            Object result;
            ChatTool tool = toolsByName.get(name);
            if (tool == null) {
                log.warn("모델이 알 수 없는 도구를 호출함: {}", name);
                result = Map.of("error", "알 수 없는 도구: " + name);
            } else {
                try {
                    result = tool.execute(args);
                } catch (Exception e) {
                    log.warn("도구 실행 실패: {}", name, e);
                    result = Map.of("error", "조회 중 오류가 발생했습니다.");
                }
            }
            responseParts.add(Map.of("functionResponse", Map.of(
                    "name", name,
                    "response", Map.of("result", result)
            )));
        }
        return responseParts;
    }

    private Map<String, Object> toFunctionDeclaration(ChatTool tool) {
        Map<String, Object> declaration = new LinkedHashMap<>();
        declaration.put("name", tool.name());
        declaration.put("description", tool.description());
        declaration.put("parameters", tool.parameters());
        return declaration;
    }

    private boolean isRetryable(Throwable throwable) {
        // 503(UNAVAILABLE)은 트래픽 급증, 429는 무료 티어 rate limit — 둘 다 짧게 재시도해볼 가치가 있다.
        if (throwable instanceof WebClientResponseException e) {
            int status = e.getStatusCode().value();
            return status == 503 || status == 429;
        }
        return false;
    }

    private String mapRole(String role) {
        // Gemini는 "user" / "model"만 허용한다. 프론트/우리 DTO의 "assistant"는 "model"로 매핑.
        return "assistant".equalsIgnoreCase(role) ? "model" : "user";
    }

    private Map<String, Object> toContent(String role, List<?> parts) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("role", role);
        content.put("parts", parts);
        return content;
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> extractContent(Map<?, ?> response) {
        if (response == null) {
            return null;
        }
        List<?> candidates = (List<?>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        Map<?, ?> first = (Map<?, ?>) candidates.get(0);
        return (Map<?, ?>) first.get("content");
    }
}
