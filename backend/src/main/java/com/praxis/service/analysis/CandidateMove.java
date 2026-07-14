package com.praxis.service.analysis;

import com.praxis.domain.enums.GamePhase;

public record CandidateMove(
    ParsedMove move,
    double materialSwing,
    GamePhase phase,
    boolean isTimePressure
) {}
