package com.praxis.api;

import com.praxis.config.AppProperties;
import com.praxis.domain.Game;
import com.praxis.domain.enums.AnalysisStatus;
import com.praxis.dto.GameSummaryDto;
import com.praxis.pipeline.AnalysisPipelineOrchestrator;
import com.praxis.repository.GameRepository;
import com.praxis.repository.MoveErrorRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/games")
public class GamesController {

    private final GameRepository gameRepository;
    private final MoveErrorRepository moveErrorRepository;
    private final AnalysisPipelineOrchestrator orchestrator;
    private final AppProperties appProperties;

    public GamesController(GameRepository gameRepository,
                           MoveErrorRepository moveErrorRepository,
                           AnalysisPipelineOrchestrator orchestrator,
                           AppProperties appProperties) {
        this.gameRepository = gameRepository;
        this.moveErrorRepository = moveErrorRepository;
        this.orchestrator = orchestrator;
        this.appProperties = appProperties;
    }

    @GetMapping
    public ResponseEntity<List<GameSummaryDto>> listGames() {
        String username = appProperties.chessCom().username();
        List<Game> games = gameRepository.findByUsernameOrderByPlayedAtDesc(username);

        Map<UUID, Integer> mistakeCounts = moveErrorRepository
                .countSuccessfulPerGameByUsername(username)
                .stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> ((Long) row[1]).intValue()));

        List<GameSummaryDto> dtos = games.stream()
                .map(g -> GameSummaryDto.from(g, mistakeCounts.getOrDefault(g.getId(), 0)))
                .toList();

        return ResponseEntity.ok(dtos);
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
