package com.praxis.service.analysis;

public record ParsedMove(
    int moveNumber,
    String san,
    String fenBefore,
    String fenAfter,
    Integer clockRemainingSeconds,
    Double evalScore   // from Chess.com %eval annotation (White's perspective); null if not available
) {}
