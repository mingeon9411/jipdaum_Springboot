package com.jipdaum_spring.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jipdaum_spring.dto.chat.ChatMessage;
import com.jipdaum_spring.service.chat.ChatTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Google AI Studio의 Gemini generateContent REST API를 호출한다.
 * HCaptchaService와 동일한 정책: api-key가 비어있는 환경(로컬 미설정)에서는 호출 자체를 건너뛰고,
 * 호출이 실패하면 예외를 던지지 않고 안전하게 empty를 반환한다 — 호출부(ChatService)가 규칙 기반
 * 답변으로 폴백할 수 있도록.
 *
 * 이 앱은 서블릿 기반 blocking MVC라(Security/JPA 전부 non-reactive) WebClient+block() 대신
 * 동기 클라이언트인 RestClient를 쓴다 — reactive pipeline을 만들었다가 바로 block()으로 되돌리는
 * 오버헤드가 없고, 이 호출 하나 때문에 webflux 의존성 전체를 끌고 다닐 필요도 없다.
 *
 * tools가 주어지면 Gemini function-calling 루프를 돈다: 모델이 functionCall을 요청하면 로컬에서
 * 해당 ChatTool을 실행하고 결과를 다시 넣어 재호출 — 최종 텍스트가 나올 때까지 반복한다.
 * (실제 API로 검증한 요구사항: functionResponse는 role "user"로 보내야 하고, 모델이 반환한
 * functionCall 파트의 thoughtSignature를 그대로 echo하지 않으면 400 에러가 난다.)
 */
@Slf4j
@Component
public class GeminiClient {

    // 프롬프트에서 "검색/상세조회는 꼭 필요할 때만, 한 번씩만"을 지시해뒀다 — 정상적인 흐름이면
    // search_products 1라운드 + 답변 1라운드로 끝난다. 그래도 고객이 특정 상품 상세를 콕 집어
    // 물어보는 경우를 위해 get_product_detail 1회 분량의 여유(3라운드째)를 남겨둔다. 2로 더
    // 낮추면 그 경우가 라운드 한도 초과로 폴백돼버린다.
    private static final int MAX_TOOL_CALL_ROUNDS = 3;

    // Gemini가 트래픽 급증 시 503(UNAVAILABLE)이나 429(rate limit)를 종종 반환한다 —
    // 짧게 1회만 재시도한다(기존 reactor Retry.backoff(1, 500ms)와 동일한 정책).
    private static final int MAX_HTTP_RETRIES = 1;
    private static final long RETRY_BACKOFF_MS = 500;

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-3.7-flash}")
    private String model;

    @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String baseUrl;

    // gemini-3.7-flash는 "thinking" 모드가 있는 모델이라 tool 선언(search_products,
    // get_product_detail)까지 붙으면 12초를 넘기는 경우가 실제로 있었다(라이브 테스트로 확인,
    // SocketTimeoutException 발생). 25초로 넉넉히 잡되, 그래도 실패하면 규칙 기반으로 폴백한다.
    // (@Value 필드는 생성자 완료 후 주입되므로, 필드 초기화 시점에 바로 못 쓰는 상수로 둔다.)
    private static final int READ_TIMEOUT_MS = 25_000;

    private final RestClient restClient = RestClient.builder()
            .requestFactory(timeoutRequestFactory())
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Optional<String> generate(String systemPrompt, List<ChatMessage> history, String userMessage,
                                      List<ChatTool> tools) {
        return generate(systemPrompt, history, userMessage, tools, Map.of());
    }

    /**
     * toolContext는 Gemini에게 노출되지 않는(functionDeclaration parameters에 없는) 고정 인자다.
     * 모델이 도구를 호출하면 그 인자에 강제로 덮어써서 넣는다 — 예: 어느 페이지(채널)의 채팅인지에
     * 따라 search_products의 검색 범위(collection)를 모델이 아니라 호출부가 결정하고 싶을 때 쓴다.
     */
    public Optional<String> generate(String systemPrompt, List<ChatMessage> history, String userMessage,
                                      List<ChatTool> tools, Map<String, Object> toolContext) {
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
                contents.add(toContent("user", executeToolCalls(functionCalls, toolsByName, toolContext)));
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

        // API 키는 쿼리스트링(?key=)이 아니라 헤더로 보낸다 — 쿼리스트링에 두면 타임아웃 등
        // I/O 예외(ResourceAccessException) 메시지에 요청 URI 전체가 그대로 실려 로그에 키가 남는다.
        String uri = baseUrl + "/models/" + model + ":generateContent";

        for (int attempt = 0; ; attempt++) {
            try {
                // .body(Map.class)로 바로 받으면 Spring이 응답 Content-Type을 보고 컨버터를 고르는데,
                // Gemini가 (특히 tools 포함 요청에서) 실제로는 JSON 본문을 주면서도 Content-Type을
                // application/octet-stream으로 잘못 내려주는 경우가 있어 역직렬화가 실패했다.
                // Content-Type과 무관하게 항상 String으로 받아 직접 JSON 파싱하도록 우회한다.
                String responseBody = restClient.post()
                        .uri(uri)
                        .header("x-goog-api-key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(String.class);
                return objectMapper.readValue(responseBody, Map.class);
            } catch (RestClientResponseException e) {
                if (attempt < MAX_HTTP_RETRIES && isRetryable(e)) {
                    sleepBackoff();
                    continue;
                }
                throw e;
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Gemini 응답 JSON 파싱 실패", e);
            }
        }
    }

    private void sleepBackoff() {
        try {
            Thread.sleep(RETRY_BACKOFF_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private List<Map<String, Object>> executeToolCalls(List<Map<?, ?>> functionCalls, Map<String, ChatTool> toolsByName,
                                                         Map<String, Object> toolContext) {
        List<Map<String, Object>> responseParts = new ArrayList<>();
        for (Map<?, ?> callPart : functionCalls) {
            Map<?, ?> functionCall = (Map<?, ?>) callPart.get("functionCall");
            String name = String.valueOf(functionCall.get("name"));
            Object rawArgs = functionCall.get("args");
            @SuppressWarnings("unchecked")
            Map<String, Object> modelArgs = rawArgs != null ? (Map<String, Object>) rawArgs : Map.of();
            // toolContext가 모델이 보낸 값을 덮어쓴다 — 모델은 애초에 이 키들을 스키마에서 본 적도 없다.
            Map<String, Object> args = new LinkedHashMap<>(modelArgs);
            args.putAll(toolContext);

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

    private boolean isRetryable(RestClientResponseException e) {
        // 503(UNAVAILABLE)은 트래픽 급증, 429는 무료 티어 rate limit — 둘 다 짧게 재시도해볼 가치가 있다.
        int status = e.getStatusCode().value();
        return status == 503 || status == 429;
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

    private static SimpleClientHttpRequestFactory timeoutRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return factory;
    }
}