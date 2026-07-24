package com.praxis.dto;

import com.praxis.domain.MoveError;

import java.util.UUID;

/**
 * A tactics drill generated from one of the player's own mistakes.
 * The user is shown {@code fen} (their position before the blunder) and must find {@code bestMove}.
 */
public record DrillDto(
        UUID id,
        String fen,
        String bestMove,      // engine move in UCI (e.g. "e2e4"), the drill solution
        String movePlayed,    // what the player actually played (SAN)
        String severity,
        String tacticalMotif,
        String gamePhase,
        String explanation,
        String playerColor,   // board orientation / side to move
        String gameId
) {
    public static DrillDto from(MoveError e) {
        return new DrillDto(
                e.getId(),
                e.getFenPosition(),
                e.getBetterMove(),
                e.getMovePlayed(),
                e.getSeverity() != null ? e.getSeverity().name() : null,
                e.getTacticalMotif() != null ? e.getTacticalMotif().name() : null,
                e.getGamePhase() != null ? e.getGamePhase().name() : null,
                e.getExplanation(),
                e.getPlayerColor(),
                e.getGame() != null ? e.getGame().getId().toString() : null);
    }
}
