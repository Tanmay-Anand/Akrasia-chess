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
    private final String moveModel;    // fast model for per-move calls (×N per game)
    private final String reportModel;  // quality model for pattern report + training plan (×1)

    public OllamaAnalysisClient(AppProperties appProperties, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        String base = appProperties.ollama().model();
        String move = appProperties.ollama().moveModel();
        String report = appProperties.ollama().reportModel();
        this.moveModel   = (move   != null && !move.isBlank())   ? move   : base;
        this.reportModel = (report != null && !report.isBlank()) ? report : base;

        this.restClient = RestClient.builder()
                .baseUrl(appProperties.ollama().baseUrl())
                .build();
    }

    // For per-move explanations — uses the fast move model
    public <T> T analyzeMove(String prompt, Class<T> responseType) {
        return analyze(moveModel, prompt, 200, responseType);
    }

    // For pattern report + training plan — uses the quality report model
    public <T> T analyzeReport(String prompt, Class<T> responseType) {
        return analyze(reportModel, prompt, 512, responseType);
    }

    private <T> T analyze(String model, String prompt, int maxTokens, Class<T> responseType) {
        OllamaRequest request = new OllamaRequest(model, prompt, false, "json", maxTokens, "2h", 2048);

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
