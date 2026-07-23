package com.praxis.service.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MoveAnalysisResult(
    String explanation,
    @JsonProperty("tactical_motif") String tacticalMotif
) {}
