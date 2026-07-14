package com.praxis.service.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MoveAnalysisResult(
    String severity,
    @JsonProperty("better_move") String betterMove,
    String explanation,
    @JsonProperty("tactical_motif") String tacticalMotif,
    @JsonProperty("phase_assessment") String phaseAssessment
) {}
