package com.praxis.pipeline;

import com.praxis.config.AppProperties;
import com.praxis.domain.Game;
import com.praxis.domain.MoveError;
import com.praxis.domain.enums.AnalysisStatus;
import com.praxis.domain.enums.GamePhase;
import com.praxis.domain.enums.Severity;
import com.praxis.domain.enums.TacticalMotif;
import com.praxis.repository.GameRepository;
import com.praxis.repository.MoveErrorRepository;
import com.praxis.service.PatternAggregator;
import com.praxis.service.ai.OllamaAnalysisClient;
import com.praxis.service.ai.PromptTemplates;
import com.praxis.service.ai.dto.MoveAnalysisResult;
import com.praxis.service.analysis.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class AnalysisPipelineOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AnalysisPipelineOrchestrator.class);

    private final PgnParserService pgnParserService;
    private final PositionEvaluator positionEvaluator;
    private final MistakeCandidateFilter candidateFilter;
    private final OllamaAnalysisClient ollamaClient;
    private final GameRepository gameRepository;
    private final MoveErrorRepository moveErrorRepository;
    private final PatternAggregator patternAggregator;
    private final AppProperties appProperties;

    public AnalysisPipelineOrchestrator(PgnParserService pgnParserService,
                                        PositionEvaluator positionEvaluator,
                                        MistakeCandidateFilter candidateFilter,
                                        OllamaAnalysisClient ollamaClient,
                                        GameRepository gameRepository,
                                        MoveErrorRepository moveErrorRepository,
                                        PatternAggregator patternAggregator,
                                        AppProperties appProperties) {
        this.pgnParserService = pgnParserService;
        this.positionEvaluator = positionEvaluator;
        this.candidateFilter = candidateFilter;
        this.ollamaClient = ollamaClient;
        this.gameRepository = gameRepository;
        this.moveErrorRepository = moveErrorRepository;
        this.patternAggregator = patternAggregator;
        this.appProperties = appProperties;
    }

    @Async("analysisExecutor")
    @Transactional
    public void analyzeGames(List<Game> games, String username) {
        log.info("Starting analysis pipeline for {} games (user: {})", games.size(), username);

        for (Game game : games) {
            try {
                analyzeGame(game);
            } catch (Exception e) {
                log.error("Pipeline failed for game {}: {}", game.getChessComId(), e.getMessage(), e);
                game.setAnalysisStatus(AnalysisStatus.FAILED);
                gameRepository.save(game);
            }
        }

        log.info("Analysis pipeline complete. Running pattern aggregation...");
        patternAggregator.recompute(username);
    }

    private void analyzeGame(Game game) {
        log.debug("Analyzing game: {}", game.getChessComId());
        game.setAnalysisStatus(AnalysisStatus.ANALYZING);
        gameRepository.save(game);

        // STATE: PARSE_PGN
        ParsedGame parsedGame = pgnParserService.parse(
                game.getId().toString(),
                game.getRawPgn(),
                appProperties.chessCom().username());

        if (parsedGame.moves().isEmpty()) {
            log.warn("No moves parsed for game {}, marking FAILED", game.getChessComId());
            game.setAnalysisStatus(AnalysisStatus.FAILED);
            gameRepository.save(game);
            return;
        }

        // Update opening info from parsed PGN headers
        if (game.getOpeningEco() == null && !parsedGame.openingEco().isEmpty()) {
            game.setOpeningEco(parsedGame.openingEco());
            game.setOpeningName(parsedGame.openingName());
        }

        // STATE: EVALUATE_POSITIONS
        List<Double> scores = positionEvaluator.evaluateAll(parsedGame.moves());

        // STATE: IDENTIFY_CANDIDATE_MISTAKES
        List<CandidateMove> candidates = candidateFilter.filter(parsedGame, scores);
        log.debug("Found {} candidate mistakes in game {}", candidates.size(), game.getChessComId());

        // STATE: OLLAMA_ANALYZE
        for (CandidateMove candidate : candidates) {
            persistMoveError(game, candidate, parsedGame.playerColor());
        }

        // STATE: PERSIST_RESULTS
        game.setAnalysisStatus(AnalysisStatus.ANALYZED);
        game.setAnalyzedAt(OffsetDateTime.now(ZoneOffset.UTC));
        gameRepository.save(game);

        log.debug("Game {} analyzed successfully", game.getChessComId());
    }

    private void persistMoveError(Game game, CandidateMove candidate, String playerColor) {
        ParsedMove move = candidate.move();
        MoveError.MoveErrorBuilder builder = MoveError.builder()
                .game(game)
                .moveNumber(move.moveNumber())
                .playerColor(playerColor)
                .movePlayed(move.san())
                .fenPosition(move.fenBefore())
                .gamePhase(candidate.phase())
                .clockRemaining(move.clockRemainingSeconds());

        try {
            String prompt = PromptTemplates.moveAnalysis(
                    move.fenBefore(), move.san(), playerColor,
                    candidate.phase().name(), move.moveNumber());

            MoveAnalysisResult result = ollamaClient.analyze(prompt, MoveAnalysisResult.class);

            builder.betterMove(result.betterMove())
                   .explanation(result.explanation())
                   .severity(parseSeverity(result.severity()))
                   .tacticalMotif(parseMotif(result.tacticalMotif()))
                   .analysisFailed(false);

        } catch (Exception e) {
            log.warn("Ollama analysis failed for move {} in game {}: {}",
                    move.moveNumber(), game.getChessComId(), e.getMessage());
            builder.severity(Severity.BLUNDER)
                   .analysisFailed(true);
        }

        moveErrorRepository.save(builder.build());
    }

    private Severity parseSeverity(String raw) {
        if (raw == null) return Severity.MISTAKE;
        try {
            return Severity.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Severity.MISTAKE;
        }
    }

    private TacticalMotif parseMotif(String raw) {
        if (raw == null) return TacticalMotif.OTHER;
        try {
            return TacticalMotif.valueOf(raw.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return TacticalMotif.OTHER;
        }
    }

    private GamePhase parsePhase(String raw) {
        if (raw == null) return GamePhase.MIDDLEGAME;
        try {
            return GamePhase.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return GamePhase.MIDDLEGAME;
        }
    }
}
