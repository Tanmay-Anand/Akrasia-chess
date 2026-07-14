package com.praxis.service.analysis;

public record ParsedMove(
    int moveNumber,
    String san,
    String fenBefore,
    String fenAfter,
    Integer clockRemainingSeconds
) {}
