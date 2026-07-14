package com.praxis.dto;

import java.util.List;
import java.util.Map;

public record DashboardStatsDto(
    int totalGames,
    int wins,
    int losses,
    int draws,
    int gamesAnalyzed,
    Map<String, Integer> openingDistribution,
    List<RatingPointDto> ratingHistory
) {}
