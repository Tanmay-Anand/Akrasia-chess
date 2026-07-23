package com.praxis.service.analysis;

import com.praxis.domain.enums.GamePhase;
import com.praxis.domain.enums.Severity;

import java.util.List;

public record CandidateMove(
    ParsedMove move,
    double materialSwing,
    GamePhase phase,
    boolean isTimePressure,
    Severity severity,       // computed from centipawn swing, never from LLM
    String bestMoveUci,      // Stockfish bestmove in UCI format (e.g. "e2e4"), null if unavailable
    double evalBefore,       // position eval (White's POV) before player's move
    double evalAfter,        // position eval (White's POV) after player's move
    List<String> engineLines // top engine continuations from MultiPV (first 4 moves each), may be empty
) {}
