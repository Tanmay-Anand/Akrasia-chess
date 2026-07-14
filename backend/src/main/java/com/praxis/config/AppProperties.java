package com.praxis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "praxis-chess")
public record AppProperties(
    Ollama ollama,
    ChessCom chessCom
) {
    public record Ollama(String baseUrl, String model) {}
    public record ChessCom(String username) {}
}
