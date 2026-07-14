package com.praxis.service.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PatternReportResult(
    @JsonProperty("primary_weakness")   String primaryWeakness,
    @JsonProperty("secondary_weakness") String secondaryWeakness,
    @JsonProperty("tertiary_weakness")  String tertiaryWeakness,
    @JsonProperty("critical_move_range") String criticalMoveRange,
    @JsonProperty("dominant_motif")     String dominantMotif,
    @JsonProperty("opening_assessment") String openingAssessment
) {}
