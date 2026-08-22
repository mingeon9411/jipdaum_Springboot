package com.jipdaum_spring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Gemini embedContent/batchEmbedContents REST API로 텍스트를 벡터로 변환한다.
 * GeminiClient(챗)와 동일한 정책: api-key가 비어있으면 호출을 건너뛰고, 실패해도 예외를 던지지
 * 않고 안전하게 empty를 반환한다 — 호출부(ProductSearchTool)가 기존 키워드 검색으로 폴백할 수
 * 있도록.
 *
 * 문서(상품)와 질의(사용자 질문)는 taskType을 다르게 줘야 검색 품질이 제대로 나온다 —
 * Gemini 임베딩 API가 비대칭 임베딩(asymmetric embedding)을 공식 지원한다.
 */
@Slf4j
@Component
public class GeminiEmbeddingClient {

    public enum TaskType { RETRIEVAL_DOCUMENT, RETRIEVAL_QUERY }

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.embedding-model:gemini-embedding-001}")
    private String model;

    @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String baseUrl;

    private final RestClient restClient = RestClient.builder()
            .requestFactory(timeoutRequestFactory())
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Optional<float[]> embed(String text, TaskType taskType) {
        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(text)) {
            return Optional.empty();
        }

        try {
            Map<String, Object> body = Map.of(
                    "content", Map.of("parts", List.of(Map.of("text", text))),
                    "taskType", taskType.name()
            );

            // GeminiClient와 동일한 이유로 Content-Type과 무관하게 String으로 받아 직접 파싱한다.
            // API 키는 쿼리스트링이 아니라 헤더로 보낸다 — 타임아웃 예외 메시지에 요청 URI가
            // 그대로 실려 로그에 키가 남는 걸 막기 위함(GeminiClient와 동일한 이유).
            String responseBody = restClient.post()
                    .uri(baseUrl + "/models/" + model + ":embedContent")
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            Map<?, ?> response = objectMapper.readValue(responseBody, Map.class);

            return response != null ? extractValues((Map<?, ?>) response.get("embedding")) : Optional.empty();
        } catch (RestClientException | com.fasterxml.jackson.core.JsonProcessingException e) {
            log.warn("Gemini 임베딩 호출 실패", e);
            return Optional.empty();
        }
    }

    /**
     * 여러 텍스트를 한 번의 API 호출로 임베딩한다(초기 전체 상품 색인용). 호출 자체가 실패하거나
     * 응답 개수가 안 맞으면 전부 empty로 반환한다 — 호출부가 항목별 오류 처리 없이 통째로
     * 안전하게 스킵하도록.
     */
    public List<Optional<float[]>> embedBatch(List<String> texts, TaskType taskType) {
        if (!StringUtils.hasText(apiKey) || texts.isEmpty()) {
            return texts.stream().map(t -> Optional.<float[]>empty()).toList();
        }

        try {
            List<Map<String, Object>> requests = texts.stream()
                    .map(text -> Map.<String, Object>of(
                            "model", "models/" + model,
                            "content", Map.of("parts", List.of(Map.of("text", text))),
                            "taskType", taskType.name()
                    ))
                    .toList();

            String responseBody = restClient.post()
                    .uri(baseUrl + "/models/" + model + ":batchEmbedContents")
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(Map.of("requests", requests))
                    .retrieve()
                    .body(String.class);
            Map<?, ?> response = objectMapper.readValue(responseBody, Map.class);

            List<?> embeddings = response != null ? (List<?>) response.get("embeddings") : null;
            if (embeddings == null || embeddings.size() != texts.size()) {
                log.warn("Gemini 배치 임베딩 응답 개수가 요청과 다릅니다.");
                return texts.stream().map(t -> Optional.<float[]>empty()).toList();
            }
            return embeddings.stream()
                    .map(e -> extractValues((Map<?, ?>) e))
                    .toList();
        } catch (RestClientException | com.fasterxml.jackson.core.JsonProcessingException e) {
            log.warn("Gemini 배치 임베딩 호출 실패", e);
            return texts.stream().map(t -> Optional.<float[]>empty()).toList();
        }
    }

    private Optional<float[]> extractValues(Map<?, ?> embedding) {
        if (embedding == null) {
            return Optional.empty();
        }
        List<?> values = (List<?>) embedding.get("values");
        if (values == null) {
            return Optional.empty();
        }
        float[] vector = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            vector[i] = ((Number) values.get(i)).floatValue();
        }
        return Optional.of(vector);
    }

    private static SimpleClientHttpRequestFactory timeoutRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(8_000);
        return factory;
    }
}
