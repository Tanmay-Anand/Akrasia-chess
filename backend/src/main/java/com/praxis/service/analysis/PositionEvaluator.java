package com.praxis.service.analysis;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PositionEvaluator {

    // Returns material score from White's perspective (positive = White advantage)
    public double evaluate(String fen) {
        Board board = new Board();
        board.loadFromFen(fen);

        double white = materialScore(board, Side.WHITE);
        double black = materialScore(board, Side.BLACK);
        return white - black;
    }

    public List<Double> evaluateAll(List<ParsedMove> moves) {
        List<Double> scores = new ArrayList<>();
        for (ParsedMove move : moves) {
            scores.add(evaluate(move.fenAfter()));
        }
        return scores;
    }

    // Material swing experienced by the player who made the move at index i.
    // Positive means the player gained material, negative means they lost material.
    public double swing(List<Double> scores, int index, boolean playerIsWhite) {
        if (index == 0) return 0;
        double before = scores.get(index - 1);
        double after  = scores.get(index);
        double delta  = after - before;
        return playerIsWhite ? delta : -delta;
    }

    private double materialScore(Board board, Side side) {
        double score = 0;
        score += Long.bitCount(board.getBitboard(side == Side.WHITE ? Piece.WHITE_QUEEN  : Piece.BLACK_QUEEN))  * 9;
        score += Long.bitCount(board.getBitboard(side == Side.WHITE ? Piece.WHITE_ROOK   : Piece.BLACK_ROOK))   * 5;
        score += Long.bitCount(board.getBitboard(side == Side.WHITE ? Piece.WHITE_BISHOP : Piece.BLACK_BISHOP)) * 3;
        score += Long.bitCount(board.getBitboard(side == Side.WHITE ? Piece.WHITE_KNIGHT : Piece.BLACK_KNIGHT)) * 3;
        score += Long.bitCount(board.getBitboard(side == Side.WHITE ? Piece.WHITE_PAWN   : Piece.BLACK_PAWN))   * 1;
        return score;
    }
}
