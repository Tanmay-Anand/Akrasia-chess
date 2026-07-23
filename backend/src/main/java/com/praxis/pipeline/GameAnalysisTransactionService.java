package com.praxis.pipeline;

import com.praxis.config.AppProperties;
import com.praxis.domain.Game;
import com.praxis.domain.MoveError;
import com.praxis.domain.enums.AnalysisState;
import com.praxis.domain.enums.AnalysisStatus;
import com.praxis.domain.enums.TacticalMotif;
import com.praxis.repository.GameRepository;
import com.praxis.repository.MoveErrorRepository;
import com.praxis.service.ai.OllamaAnalysisClient;
import com.praxis.service.ai.PromptTemplates;
import com.praxis.service.ai.dto.MoveAnalysisResult;
import com.praxis.service.analysis.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

/**
 * Wraps per-game analysis in its own REQUIRES_NEW transaction so each game commits
 * independently. A crash mid-run loses only the in-flight game; all prior games remain ANALYZED.
 */
@Service
public class GameAnalysisTransactionService {

    private static final Logger log = LoggerFactory.getLogger(GameAnalysisTransactionService.class);
    private static final int MAX_OLLAMA_CALLS_PER_GAME = 3;

    private final PgnParserService pgnParserService;
    private final PositionEvaluator positionEvaluator;
    private final MistakeCandidateFilter candidateFilter;
    private final OllamaAnalysisClient ollamaClient;
    private final GameRepository gameRepository;
    private final MoveErrorRepository moveErrorRepository;
    private final AppProperties appProperties;

    public GameAnalysisTransactionService(PgnParserService pgnParserService,
                                          PositionEvaluator positionEvaluator,
                                          MistakeCandidateFilter candidateFilter,
                                          OllamaAnalysisClient ollamaClient,
                                          GameRepository gameRepository,
                                          MoveErrorRepository moveErrorRepository,
                                          AppProperties appProperties) {
        this.pgnParserService = pgnParserService;
        this.positionEvaluator = positionEvaluator;
        this.candidateFilter = candidateFilter;
        this.ollamaClient = ollamaClient;
        this.gameRepository = gameRepository;
        this.moveErrorRepository = moveErrorRepository;
        this.appProperties = appProperties;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void analyzeOne(Game game) {
        log.debug("Analyzing game: {}", game.getChessComId());
        game.setAnalysisStatus(AnalysisStatus.ANALYZING);
        gameRepository.save(game);

        String username = appProperties.chessCom().username();
        ParsedGame parsedGame = pgnParserService.parse(
                game.getId().toString(), game.getRawPgn(), username);

        if (parsedGame.moves().isEmpty()) {
            log.warn("No moves parsed for game {}, marking FAILED", game.getChessComId());
            game.setAnalysisStatus(AnalysisStatus.FAILED);
            gameRepository.save(game);
            return;
        }

        if (game.getOpeningEco() == null && !parsedGame.openingEco().isEmpty()) {
            game.setOpeningEco(parsedGame.openingEco());
            game.setOpeningName(parsedGame.openingName());
        }

        List<Double> scores = positionEvaluator.evaluateAll(parsedGame.moves());

        // Backfill accuracy from Stockfish ACPL if Chess.com didn't provide it
        if (game.getAccuracy() == null && !scores.isEmpty()) {
            game.setAccuracy(computeAccuracy(parsedGame, scores));
        }

        List<CandidateMove> candidates = candidateFilter.filter(parsedGame, scores);
        log.debug("Found {} candidate mistakes in game {}", candidates.size(), game.getChessComId());

        List<CandidateMove> sorted = candidates.stream()
                .sorted(Comparator.comparingDouble(CandidateMove::materialSwing))
                .toList();

        for (int i = 0; i < sorted.size(); i++) {
            persistMoveError(game, sorted.get(i), parsedGame.playerColor(), i < MAX_OLLAMA_CALLS_PER_GAME);
        }

        game.setAnalysisStatus(AnalysisStatus.ANALYZED);
        game.setAnalyzedAt(OffsetDateTime.now(ZoneOffset.UTC));
        gameRepository.save(game);

        log.debug("Game {} analyzed successfully", game.getChessComId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Game game) {
        game.setAnalysisStatus(AnalysisStatus.FAILED);
        gameRepository.save(game);
    }

    private void persistMoveError(Game game, CandidateMove candidate, String playerColor, boolean callOllama) {
        ParsedMove move = candidate.move();

        MoveError.MoveErrorBuilder builder = MoveError.builder()
                .game(game)
                .moveNumber(move.moveNumber())
                .playerColor(playerColor)
                .movePlayed(move.san())
                .betterMove(candidate.bestMoveUci())   // engine-provided UCI (e.g. "e2e4")
                .fenPosition(move.fenBefore())
                .severity(candidate.severity())         // engine-computed from centipawn swing
                .gamePhase(candidate.phase())
                .clockRemaining(move.clockRemainingSeconds());

        if (callOllama) {
            try {
                String prompt = PromptTemplates.moveAnalysis(
                        move.fenBefore(), move.san(), candidate.bestMoveUci(),
                        candidate.evalBefore(), candidate.evalAfter(),
                        playerColor, candidate.phase().name(), move.moveNumber(),
                        candidate.engineLines());

                MoveAnalysisResult result = ollamaClient.analyzeMove(prompt, MoveAnalysisResult.class);
                builder.explanation(result.explanation())
                       .tacticalMotif(parseMotif(result.tacticalMotif()))
                       .analysisState(AnalysisState.EXPLAINED);

            } catch (Exception e) {
                log.warn("Ollama analysis failed for move {} in game {}: {}",
                        move.moveNumber(), game.getChessComId(), e.getMessage());
                builder.analysisState(AnalysisState.LLM_FAILED);
            }
        } else {
            builder.analysisState(AnalysisState.SKIPPED);
        }

        moveErrorRepository.save(builder.build());
    }

    // Computes accuracy % from average centipawn loss across all player moves.
    // Formula approximates Chess.com's accuracy curve.
    private double computeAccuracy(ParsedGame game, List<Double> scores) {
        boolean playerIsWhite = "white".equals(game.playerColor());
        double totalLoss = 0.0;
        int moveCount = 0;

        for (int i = 0; i < game.moves().size(); i++) {
            ParsedMove move = game.moves().get(i);
            boolean isWhiteMove = (move.moveNumber() % 2 == 1);
            if (playerIsWhite != isWhiteMove) continue;

            int afterIdx  = Math.min(i, scores.size() - 1);
            int beforeIdx = Math.max(0, i - 1);
            double evalAfter  = scores.get(afterIdx);
            double evalBefore = scores.get(beforeIdx);

            double swing = playerIsWhite ? (evalAfter - evalBefore) : (evalBefore - evalAfter);
            totalLoss += Math.max(0.0, -swing);
            moveCount++;
        }

        if (moveCount == 0) return 0.0;
        double acpl = Math.min((totalLoss / moveCount) * 100.0, 500.0); // centipawns, capped at +5 pawns avg
        double accuracy = 103.1668 * Math.exp(-0.04354 * acpl) - 3.1668;
        return Math.round(Math.max(0.0, Math.min(100.0, accuracy)) * 10.0) / 10.0;
    }

    private TacticalMotif parseMotif(String raw) {
        if (raw == null) return TacticalMotif.OTHER;
        try {
            return TacticalMotif.valueOf(raw.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return TacticalMotif.OTHER;
        }
    }
}
