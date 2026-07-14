package com.praxis.dto;

import com.praxis.domain.MoveError;

import java.util.UUID;

public record MoveErrorDto(
    UUID id,
    int moveNumber,
    String movePlayed,
    String betterMove,
    String fenPosition,
    String severity,
    String tacticalMotif,
    String explanation,
    String gamePhase,
    Integer clockRemaining,
    boolean analysisFailed
) {
    public static MoveErrorDto from(MoveError e) {
        return new MoveErrorDto(
                e.getId(), e.getMoveNumber(), e.getMovePlayed(), e.getBetterMove(),
                e.getFenPosition(), e.getSeverity() != null ? e.getSeverity().name() : null,
                e.getTacticalMotif() != null ? e.getTacticalMotif().name() : null,
                e.getExplanation(),
                e.getGamePhase() != null ? e.getGamePhase().name() : null,
                e.getClockRemaining(), e.isAnalysisFailed());
    }
}
