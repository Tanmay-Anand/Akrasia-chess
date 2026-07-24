package com.praxis.dto;

import java.util.List;

/**
 * Aggregated practical analytics served at GET /api/insights.
 * Everything is derived from already-persisted Game + MoveError data.
 */
public record InsightsDto(
        List<OpponentBucket> opponentStrength,
        List<AccuracyTrendPoint> accuracyTrend,
        List<TimeBucket> timeOfDay,
        List<TimeBucket> dayOfWeek,
        PhaseAccuracy phaseAccuracy,
        TimeManagement timeManagement,
        Conversion conversion,
        List<MotifCount> missedTactics,
        Tilt tilt,
        List<OpeningInsight> openings
) {

    // Win rate + accuracy vs stronger / even / weaker opponents.
    public record OpponentBucket(String bucket, int games, int wins, double winPct, Double avgAccuracy) {}

    // Per-game accuracy over time with a rolling average.
    public record AccuracyTrendPoint(String date, double accuracy, double movingAvg) {}

    // Win rate grouped by a time bucket (part of day or weekday).
    public record TimeBucket(String label, int games, int wins, double winPct) {}

    // Mistake counts per game phase.
    public record PhaseAccuracy(int opening, int middlegame, int endgame) {}

    // Time-trouble blunder analytics.
    public record TimeManagement(
            Double avgMoveSeconds,
            int totalBlunders,
            int blundersInTimePressure,
            double timeTroubleRate) {}

    // Winning-position conversion.
    public record Conversion(int winningGames, int converted, double conversionPct, List<BlownGame> blownGames) {}

    public record BlownGame(String gameId, String openingName, double maxAdvantage, String result, String playedAt) {}

    // Frequency of missed tactical motifs.
    public record MotifCount(String motif, int count) {}

    // Performance in the game immediately after a win vs after a loss.
    public record Tilt(int afterWinGames, double afterWinWinPct, int afterLossGames, double afterLossWinPct) {}

    // Win rate + accuracy per opening (ECO).
    public record OpeningInsight(String eco, String name, int games, double winPct, Double avgAccuracy) {}
}
