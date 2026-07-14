package com.praxis.dto;

import com.praxis.domain.PlayerPattern;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PatternDto(
    UUID id,
    int gamesAnalyzed,
    OffsetDateTime computedAt,
    int mistakesMoves1to10,
    int mistakesMoves11to20,
    int mistakesMoves21to30,
    int mistakesMoves31Plus,
    int mistakesOpening,
    int mistakesMiddlegame,
    int mistakesEndgame,
    String motifFrequency,
    String openingAccuracy,
    String primaryWeakness,
    String secondaryWeakness,
    String tertiaryWeakness,
    String criticalMoveRange,
    String dominantMotif,
    String openingAssessment
) {
    public static PatternDto from(PlayerPattern p) {
        return new PatternDto(
                p.getId(), p.getGamesAnalyzed(), p.getComputedAt(),
                p.getMistakesMoves1to10(), p.getMistakesMoves11to20(),
                p.getMistakesMoves21to30(), p.getMistakesMoves31Plus(),
                p.getMistakesOpening(), p.getMistakesMiddlegame(), p.getMistakesEndgame(),
                p.getMotifFrequency(), p.getOpeningAccuracy(),
                p.getPrimaryWeakness(), p.getSecondaryWeakness(), p.getTertiaryWeakness(),
                p.getCriticalMoveRange(), p.getDominantMotif(), p.getOpeningAssessment());
    }
}
