package com.praxis.api;

import com.praxis.config.AppProperties;
import com.praxis.domain.Game;
import com.praxis.domain.enums.AnalysisStatus;
import com.praxis.dto.AnalysisProgressDto;
import com.praxis.dto.MoveErrorDto;
import com.praxis.pipeline.AnalysisPipelineOrchestrator;
import com.praxis.pipeline.AnalysisProgressTracker;
import com.praxis.repository.GameRepository;
import com.praxis.repository.MoveErrorRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final MoveErrorRepository moveErrorRepository;
    private final GameRepository gameRepository;
    private final AnalysisPipelineOrchestrator pipelineOrchestrator;
    private final AppProperties appProperties;
    private final AnalysisProgressTracker progressTracker;

    public AnalysisController(MoveErrorRepository moveErrorRepository,
                              GameRepository gameRepository,
                              AnalysisPipelineOrchestrator pipelineOrchestrator,
                              AppProperties appProperties,
                              AnalysisProgressTracker progressTracker) {
        this.moveErrorRepository = moveErrorRepository;
        this.gameRepository = gameRepository;
        this.pipelineOrchestrator = pipelineOrchestrator;
        this.appProperties = appProperties;
        this.progressTracker = progressTracker;
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<List<MoveErrorDto>> getMoveErrors(@PathVariable UUID gameId) {
        List<MoveErrorDto> errors = moveErrorRepository.findByGameId(gameId)
                .stream()
                .map(MoveErrorDto::from)
                .toList();
        return ResponseEntity.ok(errors);
    }

    @GetMapping("/progress")
    public ResponseEntity<AnalysisProgressDto> getProgress() {
        int completed = progressTracker.getCompleted();
        int total = progressTracker.getTotal();
        int pct = total > 0 ? (int) ((completed * 100.0) / total) : 0;
        return ResponseEntity.ok(new AnalysisProgressDto(
                progressTracker.isRunning(),
                progressTracker.isPatternGenerating(),
                progressTracker.isQueued(),
                completed, total, pct,
                progressTracker.getEtaSeconds()));
    }

    @Transactional
    @PostMapping("/reanalyze")
    public ResponseEntity<Map<String, Object>> reanalyzeAll() {
        String username = appProperties.chessCom().username();
        List<Game> games = gameRepository.findByUsernameOrderByPlayedAtDesc(username);

        for (Game game : games) {
            moveErrorRepository.deleteByGameId(game.getId());
            game.setAnalysisStatus(AnalysisStatus.PENDING);
            game.setAnalyzedAt(null);
            gameRepository.save(game);
        }

        if (!games.isEmpty()) {
            progressTracker.setQueued(true);
            pipelineOrchestrator.analyzeGames(games, username);
        }

        return ResponseEntity.ok(Map.of(
                "message", "Reanalysis queued",
                "games_queued", games.size()));
    }
}
