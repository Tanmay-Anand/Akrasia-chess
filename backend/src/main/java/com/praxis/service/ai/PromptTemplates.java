package com.praxis.service.ai;

public final class PromptTemplates {

    private PromptTemplates() {}

    public static String moveAnalysis(String fenBefore, String sanMove, String playerColor,
                                      String phase, int moveNumber) {
        return """
            Chess coach. Identify this mistake briefly.

            FEN: %s
            Move: %s | Color: %s | Phase: %s | Move #%d

            Respond ONLY with JSON, no extra text:
            {
              "severity": "BLUNDER" | "MISTAKE" | "INACCURACY",
              "better_move": "<SAN>",
              "explanation": "<one sentence: why bad and what to play instead>",
              "tactical_motif": "FORK" | "PIN" | "SKEWER" | "BACK_RANK" | "DISCOVERED_ATTACK" | "HANGING_PIECE" | "POSITIONAL" | "OTHER"
            }
            """.formatted(fenBefore, sanMove, playerColor, phase, moveNumber);
    }

    public static String patternReport(int gameCount, int totalMistakes,
                                       String moveRangeDistribution,
                                       String motifFrequencyMap,
                                       String openingDeviationData) {
        return """
            You are a chess coach reviewing aggregated mistake data for a player.

            Games analyzed: %d
            Total mistakes flagged: %d

            Mistake breakdown by move range:
            %s

            Tactical motif frequency:
            %s

            Opening deviation summary:
            %s

            Identify the player's top 3 recurring weaknesses. Respond ONLY with this JSON schema:
            {
              "primary_weakness": "<one sentence>",
              "secondary_weakness": "<one sentence>",
              "tertiary_weakness": "<one sentence>",
              "critical_move_range": "<e.g. moves 10-20>",
              "dominant_motif": "<motif name>",
              "opening_assessment": "<one sentence about opening accuracy>"
            }
            """.formatted(gameCount, totalMistakes, moveRangeDistribution, motifFrequencyMap, openingDeviationData);
    }

    public static String trainingPlan(String patternReportJson, String openingStatsJson) {
        return """
            You are a chess coach generating a prioritized improvement plan.

            Player weaknesses identified:
            %s

            Opening performance:
            %s

            Generate a concrete training plan. Respond ONLY with this JSON schema:
            {
              "priority_1": {
                "focus": "<what to work on>",
                "action": "<specific drill or study task>",
                "reason": "<why this is the top priority>"
              },
              "priority_2": {
                "focus": "<what to work on>",
                "action": "<specific drill or study task>",
                "reason": "<why this is the second priority>"
              },
              "priority_3": {
                "focus": "<what to work on>",
                "action": "<specific drill or study task>",
                "reason": "<why this is the third priority>"
              },
              "openings_to_drill": ["<ECO code: name>"],
              "tactical_patterns_to_study": ["<pattern name>"]
            }
            """.formatted(patternReportJson, openingStatsJson);
    }
}
