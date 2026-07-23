package com.praxis.service.ai.dto;

public record OllamaRequest(
    String model,
    String prompt,
    boolean stream,
    String format,
    int numPredict,
    String keepAlive,
    Integer numCtx
) {}
