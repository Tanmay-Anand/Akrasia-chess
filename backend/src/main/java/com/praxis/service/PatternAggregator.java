package com.praxis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.praxis.domain.MoveError;
import com.praxis.domain.PlayerPattern;
import com.praxis.domain.enums.GamePhase;
import com.praxis.domain.enums.TacticalMotif;
import com.praxis.repository.GameRepository;
import com.praxis.repository.MoveErrorRepository;
import com.praxis.repository.PlayerPatternRepository;
import com.praxis.service.ai.OllamaAnalysisClient;
import com.praxis.service.ai.PromptTemplates;
import com.praxis.service.ai.dto.PatternReportResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PatternAggregator {

    private static final Logger log = LoggerFactory.getLogger(PatternAggregator.class);

    private final MoveErrorRepository moveErrorRepository;
    private final GameRepository gameRepository;
    private final PlayerPatternRepository playerPatternRepository;
    private final OllamaAnalysisClient ollamaClient;
    private final ObjectMapper objectMapper;

    public PatternAggregator(MoveErrorRepository moveErrorRepository,
                             GameRepository gameRepository,
                             PlayerPatternRepository playerPatternRepository,
                             OllamaAnalysisClient ollamaClient,
                             ObjectMapper objectMapper) {
        this.moveErrorRepository = moveErrorRepository;
        this.gameRepository = gameRepository;
        this.playerPatternRepository = playerPatternRepository;
        this.ollamaClient = ollamaClient;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void recompute(String username) {
        log.info("Recomputing patterns for {}", username);

        List<MoveError> errors = moveErrorRepository.findSuccessfulByUsername(username);
        if (errors.isEmpty()) {
            log.info("No move errors found for {}, skipping pattern aggregation", username);
            return;
        }

        int gamesAnalyzed = (int) gameRepository.countByUsernameAndAnalysisStatus(
                username, com.praxis.domain.enums.AnalysisStatus.ANALYZED);

        // Move range distribution
        int m1to10  = (int) errors.stream().filter(e -> e.getMoveNumber() <= 10).count();
        int m11to20 = (int) errors.stream().filter(e -> e.getMoveNumber() >= 11 && e.getMoveNumber() <= 20).count();
        int m21to30 = (int) errors.stream().filter(e -> e.getMoveNumber() >= 21 && e.getMoveNumber() <= 30).count();
        int m31plus = (int) errors.stream().filter(e -> e.getMoveNumber() > 30).count();

        // Phase breakdown
        int opening    = (int) errors.stream().filter(e -> e.getGamePhase() == GamePhase.OPENING).count();
        int middlegame = (int) errors.stream().filter(e -> e.getGamePhase() == GamePhase.MIDDLEGAME).count();
        int endgame    = (int) errors.stream().filter(e -> e.getGamePhase() == GamePhase.ENDGAME).count();

        // Motif frequency
        Map<String, Long> motifFreqRaw = errors.stream()
                .filter(e -> e.getTacticalMotif() != null)
                .collect(Collectors.groupingBy(e -> e.getTacticalMotif().name(), Collectors.counting()));
        Map<String, Integer> motifFreq = motifFreqRaw.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().intValue()));

        String motifFreqJson  = toJson(motifFreq);
        String openingAccJson = buildOpeningAccuracyJson(username);

        // Call Ollama for LLM-generated pattern summary
        String moveRangeDist = "Moves 1-10: %d, Moves 11-20: %d, Moves 21-30: %d, Moves 31+: %d"
                .formatted(m1to10, m11to20, m21to30, m31plus);
        String prompt = PromptTemplates.patternReport(
                gamesAnalyzed, errors.size(), moveRangeDist, motifFreqJson, openingAccJson);

        PatternReportResult llmResult = null;
        try {
            llmResult = ollamaClient.analyze(prompt, PatternReportResult.class);
        } catch (Exception e) {
            log.warn("Ollama pattern report failed: {}", e.getMessage());
        }

        PlayerPattern pattern = PlayerPattern.builder()
                .username(username)
                .gamesAnalyzed(gamesAnalyzed)
                .computedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .mistakesMoves1to10(m1to10)
                .mistakesMoves11to20(m11to20)
                .mistakesMoves21to30(m21to30)
                .mistakesMoves31Plus(m31plus)
                .mistakesOpening(opening)
                .mistakesMiddlegame(middlegame)
                .mistakesEndgame(endgame)
                .motifFrequency(motifFreqJson)
                .openingAccuracy(openingAccJson)
                .primaryWeakness(llmResult != null ? llmResult.primaryWeakness() : null)
                .secondaryWeakness(llmResult != null ? llmResult.secondaryWeakness() : null)
                .tertiaryWeakness(llmResult != null ? llmResult.tertiaryWeakness() : null)
                .criticalMoveRange(llmResult != null ? llmResult.criticalMoveRange() : null)
                .dominantMotif(llmResult != null ? llmResult.dominantMotif() : null)
                .openingAssessment(llmResult != null ? llmResult.openingAssessment() : null)
                .build();

        playerPatternRepository.save(pattern);
        log.info("Pattern aggregation complete for {}", username);
    }

    private String buildOpeningAccuracyJson(String username) {
        // Simplified: group errors by opening eco from joined game
        Map<String, Long> ecoErrors = moveErrorRepository.findSuccessfulByUsername(username).stream()
                .filter(e -> e.getGame().getOpeningEco() != null)
                .collect(Collectors.groupingBy(
                        e -> e.getGame().getOpeningEco(),
                        Collectors.counting()));
        return toJson(ecoErrors);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
