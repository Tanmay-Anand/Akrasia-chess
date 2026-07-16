package com.praxis.service.analysis;

import com.praxis.domain.enums.GamePhase;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class MistakeCandidateFilter {

    private static final double BLUNDER_THRESHOLD    = 2.0;
    private static final double MISTAKE_THRESHOLD    = 1.0;
    private static final int    TIME_PRESSURE_CUTOFF = 30; // seconds
    private static final int    MAX_CANDIDATES       = 8;

    public List<CandidateMove> filter(ParsedGame game, List<Double> scores) {
        boolean playerIsWhite = "white".equals(game.playerColor());
        List<CandidateMove> candidates = new ArrayList<>();

        for (int i = 0; i < game.moves().size(); i++) {
            ParsedMove move = game.moves().get(i);

            // Only analyse the player's own moves
            boolean isWhiteMove = (move.moveNumber() % 2 == 1);
            if (playerIsWhite != isWhiteMove) continue;

            double swing = evaluatorSwing(scores, i, playerIsWhite);
            // Negative swing means the player's position got worse
            if (swing >= -MISTAKE_THRESHOLD) continue;

            boolean timePressure = move.clockRemainingSeconds() != null
                    && move.clockRemainingSeconds() <= TIME_PRESSURE_CUTOFF;

            candidates.add(new CandidateMove(move, swing, detectPhase(move.moveNumber()), timePressure));
        }

        // Sort by worst swing first, take top N
        candidates.sort(Comparator.comparingDouble(CandidateMove::materialSwing));
        return candidates.subList(0, Math.min(candidates.size(), MAX_CANDIDATES));
    }

    private double evaluatorSwing(List<Double> scores, int moveIndex, boolean playerIsWhite) {
        if (moveIndex == 0 || scores.isEmpty()) return 0;
        int afterIdx  = Math.min(moveIndex, scores.size() - 1);
        int beforeIdx = Math.max(0, moveIndex - 1);
        double delta = scores.get(afterIdx) - scores.get(beforeIdx);
        return playerIsWhite ? delta : -delta;
    }

    private GamePhase detectPhase(int moveNumber) {
        if (moveNumber <= 10)  return GamePhase.OPENING;
        if (moveNumber <= 30)  return GamePhase.MIDDLEGAME;
        return GamePhase.ENDGAME;
    }
}
