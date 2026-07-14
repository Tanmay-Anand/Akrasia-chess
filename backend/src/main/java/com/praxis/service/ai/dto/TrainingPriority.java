package com.praxis.service.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TrainingPriority(String focus, String action, String reason) {}
