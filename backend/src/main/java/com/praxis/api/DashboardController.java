package com.praxis.api;

import com.praxis.config.AppProperties;
import com.praxis.domain.Game;
import com.praxis.domain.enums.AnalysisStatus;
import com.praxis.dto.DashboardStatsDto;
import com.praxis.dto.RatingPointDto;
import com.praxis.repository.GameRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final GameRepository gameRepository;
    private final AppProperties appProperties;

    public DashboardController(GameRepository gameRepository, AppProperties appProperties) {
        this.gameRepository = gameRepository;
        this.appProperties = appProperties;
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getStats() {
        String username = appProperties.chessCom().username();
        List<Game> games = gameRepository.findByUsernameOrderByPlayedAtDesc(username);

        int wins   = (int) games.stream().filter(g -> "win".equals(g.getResult())).count();
        int losses = (int) games.stream().filter(g -> "loss".equals(g.getResult())).count();
        int draws  = (int) games.stream().filter(g -> "draw".equals(g.getResult())).count();
        int analyzed = (int) games.stream()
                .filter(g -> g.getAnalysisStatus() == AnalysisStatus.ANALYZED).count();

        Map<String, Integer> openingDist = new LinkedHashMap<>();
        games.stream()
                .filter(g -> g.getOpeningEco() != null)
                .collect(Collectors.groupingBy(Game::getOpeningEco, Collectors.counting()))
                .forEach((eco, count) -> openingDist.put(eco, count.intValue()));

        List<RatingPointDto> ratingHistory = games.stream()
                .filter(g -> g.getPlayedAt() != null)
                .map(g -> new RatingPointDto(g.getPlayedAt(),
                        "white".equals(g.getPlayerColor())
                                ? (g.getWhiteRating() != null ? g.getWhiteRating() : 0)
                                : (g.getBlackRating() != null ? g.getBlackRating() : 0)))
                .sorted(java.util.Comparator.comparing(RatingPointDto::date))
                .toList();

        return ResponseEntity.ok(new DashboardStatsDto(
                games.size(), wins, losses, draws, analyzed, openingDist, ratingHistory));
    }

    @GetMapping("/rating-history")
    public ResponseEntity<List<RatingPointDto>> getRatingHistory() {
        String username = appProperties.chessCom().username();
        List<RatingPointDto> history = gameRepository
                .findByUsernameOrderByPlayedAtDesc(username).stream()
                .filter(g -> g.getPlayedAt() != null)
                .map(g -> new RatingPointDto(g.getPlayedAt(),
                        "white".equals(g.getPlayerColor())
                                ? (g.getWhiteRating() != null ? g.getWhiteRating() : 0)
                                : (g.getBlackRating() != null ? g.getBlackRating() : 0)))
                .sorted(java.util.Comparator.comparing(RatingPointDto::date))
                .toList();
        return ResponseEntity.ok(history);
    }
}
