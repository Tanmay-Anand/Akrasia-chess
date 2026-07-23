package com.praxis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "praxis-chess")
public record AppProperties(
    Ollama ollama,
    ChessCom chessCom,
    Stockfish stockfish
) {
    public record Ollama(String baseUrl, String model, String moveModel, String reportModel) {}
    public record ChessCom(String username) {}
    public record Stockfish(String path) {}

    public String stockfishPath() {
        return stockfish != null && stockfish.path() != null ? stockfish.path() : "";
    }
}
