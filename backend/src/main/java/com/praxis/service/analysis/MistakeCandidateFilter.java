package com.praxis.service.analysis;

import com.praxis.domain.enums.GamePhase;
import com.praxis.domain.enums.Severity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class MistakeCandidateFilter {

    private static final double BLUNDER_THRESHOLD    = 2.0;
    private static final double MISTAKE_THRESHOLD    = 1.0;
    private static final int    TIME_PRESSURE_CUTOFF = 30;
    private static final int    MAX_CANDIDATES       = 8;
    private static final int    BOOK_MOVES_PLY       = 12; // skip first 6 full moves

    // Fast first pass: score-based candidate selection, no Stockfish calls.
    // Callers are responsible for enriching candidates with MultiPV bestmove + engine lines.
    public List<CandidateMove> filterCandidates(ParsedGame game, List<Double> scores) {
        boolean playerIsWhite = "white".equals(game.playerColor());
        List<CandidateMove> candidates = new ArrayList<>();

        for (int i = 0; i < game.moves().size(); i++) {
            ParsedMove move = game.moves().get(i);

            if (move.moveNumber() <= BOOK_MOVES_PLY) continue;

            boolean isWhiteMove = (move.moveNumber() % 2 == 1);
            if (playerIsWhite != isWhiteMove) continue;

            int afterIdx  = Math.min(i, scores.size() - 1);
            int beforeIdx = Math.max(0, i - 1);
            double evalAfterRaw  = scores.get(afterIdx);
            double evalBeforeRaw = scores.get(beforeIdx);

            double swing = playerIsWhite
                    ? evalAfterRaw - evalBeforeRaw
                    : evalBeforeRaw - evalAfterRaw;

            if (swing >= -MISTAKE_THRESHOLD) continue;

            Severity severity = swing <= -BLUNDER_THRESHOLD ? Severity.BLUNDER : Severity.MISTAKE;
            boolean timePressure = move.clockRemainingSeconds() != null
                    && move.clockRemainingSeconds() <= TIME_PRESSURE_CUTOFF;

            candidates.add(new CandidateMove(
                    move, swing, detectPhase(move.moveNumber()), timePressure,
                    severity, null, evalBeforeRaw, evalAfterRaw, List.of()));
        }

        candidates.sort(Comparator.comparingDouble(CandidateMove::materialSwing));
        return candidates.subList(0, Math.min(candidates.size(), MAX_CANDIDATES));
    }

    private GamePhase detectPhase(int moveNumber) {
        if (moveNumber <= 10)  return GamePhase.OPENING;
        if (moveNumber <= 30)  return GamePhase.MIDDLEGAME;
        return GamePhase.ENDGAME;
    }
}
