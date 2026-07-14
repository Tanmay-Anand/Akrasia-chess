package com.praxis.api;

import com.praxis.config.AppProperties;
import com.praxis.domain.Game;
import com.praxis.domain.enums.AnalysisStatus;
import com.praxis.dto.GameSummaryDto;
import com.praxis.pipeline.AnalysisPipelineOrchestrator;
import com.praxis.repository.GameRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/games")
public class GamesController {

    private final GameRepository gameRepository;
    private final AnalysisPipelineOrchestrator orchestrator;
    private final AppProperties appProperties;

    public GamesController(GameRepository gameRepository,
                           AnalysisPipelineOrchestrator orchestrator,
                           AppProperties appProperties) {
        this.gameRepository = gameRepository;
        this.orchestrator = orchestrator;
        this.appProperties = appProperties;
    }

    @GetMapping
    public ResponseEntity<List<GameSummaryDto>> listGames() {
        List<GameSummaryDto> games = gameRepository
                .findByUsernameOrderByPlayedAtDesc(appProperties.chessCom().username())
                .stream()
                .map(GameSummaryDto::from)
                .toList();
        return ResponseEntity.ok(games);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GameSummaryDto> getGame(@PathVariable UUID id) {
        return gameRepository.findById(id)
                .map(GameSummaryDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/analyze")
    public ResponseEntity<Map<String, String>> reanalyze(@PathVariable UUID id) {
        return gameRepository.findById(id).map(game -> {
            game.setAnalysisStatus(AnalysisStatus.PENDING);
            gameRepository.save(game);
            orchestrator.analyzeGames(List.of(game), appProperties.chessCom().username());
            return ResponseEntity.ok(Map.of("message", "Analysis queued for game " + id));
        }).orElse(ResponseEntity.notFound().build());
    }
}
