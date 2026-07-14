package com.praxis.service.ai.dto;

public record OllamaRequest(String model, String prompt, boolean stream, String format) {}
