package com.praxis.dto;

import com.praxis.domain.TrainingPlan;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TrainingPlanDto(
    UUID id,
    OffsetDateTime generatedAt,
    int basedOnGames,
    String planJson,
    String openingsToDrill,
    String tacticalPatterns
) {
    public static TrainingPlanDto from(TrainingPlan p) {
        return new TrainingPlanDto(p.getId(), p.getGeneratedAt(), p.getBasedOnGames(),
                p.getPlanJson(), p.getOpeningsToDrill(), p.getTacticalPatterns());
    }
}
