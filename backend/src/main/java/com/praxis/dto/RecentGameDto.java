package com.praxis.dto;

public record RecentGameDto(
    String id,
    String playedAt,
    String timeClass,
    String playerColor,
    String result,
    String openingEco,
    String openingName,
    Double accuracy
) {}
