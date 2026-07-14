package com.praxis.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.praxis.config.AppProperties;
import com.praxis.service.ai.dto.OllamaGenerateResponse;
import com.praxis.service.ai.dto.OllamaRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OllamaAnalysisClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaAnalysisClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public OllamaAnalysisClient(AppProperties appProperties, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.model = appProperties.ollama().model();
        this.restClient = RestClient.builder()
                .baseUrl(appProperties.ollama().baseUrl())
                .build();
    }

    public <T> T analyze(String prompt, Class<T> responseType) {
        OllamaRequest request = new OllamaRequest(model, prompt, false, "json");

        OllamaGenerateResponse raw = restClient.post()
                .uri("/api/generate")
                .body(request)
                .retrieve()
                .body(OllamaGenerateResponse.class);

        if (raw == null || raw.response() == null) {
            throw new RuntimeException("Ollama returned null response");
        }

        String json = raw.response()
                .replaceAll("```json", "")
                .replaceAll("```", "")
                .trim();

        try {
            return objectMapper.readValue(json, responseType);
        } catch (Exception e) {
            log.warn("Failed to parse Ollama response as {}: {}", responseType.getSimpleName(), json);
            throw new RuntimeException("Ollama response parse failed: " + e.getMessage(), e);
        }
    }
}
