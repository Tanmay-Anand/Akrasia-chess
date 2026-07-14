package com.praxis.service.analysis;

import java.util.List;

public record ParsedGame(
    String gameId,
    String openingEco,
    String openingName,
    int totalMoves,
    String result,
    String playerColor,
    List<ParsedMove> moves
) {}
