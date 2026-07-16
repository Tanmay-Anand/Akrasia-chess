package com.praxis.dto;

import java.util.List;

public record DashboardStatsDto(
    int totalGames,
    int wins,
    int losses,
    int draws,
    int gamesAnalyzed,
    int currentRating,
    int ratingDelta,
    Double avgAccuracy,
    Double bestAccuracy,
    int blunderCount,
    List<OpeningStatDto> openingStats,
    List<RatingPointDto> ratingHistory,
    List<RecentGameDto> recentGames,
    // Coach page additions
    int whiteGames,
    double whiteWinPct,
    int blackGames,
    double blackWinPct,
    int formStreak,
    List<TimeControlStatDto> timeControlStats
) {}
