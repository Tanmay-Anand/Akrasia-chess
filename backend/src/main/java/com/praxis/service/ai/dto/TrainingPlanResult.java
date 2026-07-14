package com.praxis.service.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TrainingPlanResult(
    @JsonProperty("priority_1") TrainingPriority priority1,
    @JsonProperty("priority_2") TrainingPriority priority2,
    @JsonProperty("priority_3") TrainingPriority priority3,
    @JsonProperty("openings_to_drill") List<String> openingsToDrill,
    @JsonProperty("tactical_patterns_to_study") List<String> tacticalPatternsToStudy
) {}
