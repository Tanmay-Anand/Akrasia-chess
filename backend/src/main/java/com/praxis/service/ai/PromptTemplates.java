package com.praxis.service.ai;

import java.util.List;

public final class PromptTemplates {

    private PromptTemplates() {}

    /**
     * Per-move explanation prompt. Engine supplies better_move, eval delta, and top lines;
     * LLM only needs to explain WHY and classify the tactical motif.
     */
    public static String moveAnalysis(String fenBefore, String sanPlayed, String bestMoveUci,
                                      double evalBefore, double evalAfter,
                                      String playerColor, String phase, int moveNumber,
                                      List<String> engineLines) {
        String engineMove = bestMoveUci != null ? bestMoveUci : "unknown";
        double lost = Math.abs(evalAfter - evalBefore);

        StringBuilder linesSection = new StringBuilder();
        if (!engineLines.isEmpty()) {
            linesSection.append("\nEngine top lines (UCI, first 4 moves each):");
            for (int i = 0; i < engineLines.size(); i++) {
                linesSection.append("\n  ").append(i + 1).append(". ").append(engineLines.get(i));
            }
        }

        return """
            You are a chess coach. Explain concisely why the played move is worse than the engine's move.
            Do NOT suggest moves — the engine move is already determined.

            Position (FEN): %s
            Move played: %s  (eval: %+.2f → %+.2f, lost %.2f pawns)
            Engine best:  %s%s
            Phase: %s | Move #%d | Player: %s

            Respond ONLY with JSON:
            {
              "explanation": "<one sentence: why the played move is bad>",
              "tactical_motif": "FORK" | "PIN" | "SKEWER" | "BACK_RANK" | "DISCOVERED_ATTACK" | "HANGING_PIECE" | "POSITIONAL" | "OTHER"
            }
            """.formatted(fenBefore, sanPlayed, evalBefore, evalAfter, lost,
                          engineMove, linesSection, phase, moveNumber, playerColor);
    }

    public static String patternReport(int gameCount, int totalMistakes,
                                       String moveRangeDistribution,
                                       String timePressureDistribution,
                                       String motifFrequencyMap,
                                       String openingDeviationData) {
        return """
            You are a chess coach reviewing aggregated mistake data for a player.
            Base every claim ONLY on the statistics provided. Do not infer causes not present in the data.

            Games analyzed: %d
            Total mistakes flagged: %d

            Mistake breakdown by move range:
            %s

            Time-pressure mistakes (clock ≤ 30s) by phase:
            %s

            Tactical motif frequency (LLM-explained mistakes only):
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
            """.formatted(gameCount, totalMistakes, moveRangeDistribution,
                          timePressureDistribution, motifFrequencyMap, openingDeviationData);
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
