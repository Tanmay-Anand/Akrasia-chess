package com.praxis.api;

import com.praxis.config.AppProperties;
import com.praxis.dto.PatternDto;
import com.praxis.repository.PlayerPatternRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patterns")
public class PatternsController {

    private final PlayerPatternRepository playerPatternRepository;
    private final AppProperties appProperties;

    public PatternsController(PlayerPatternRepository playerPatternRepository, AppProperties appProperties) {
        this.playerPatternRepository = playerPatternRepository;
        this.appProperties = appProperties;
    }

    @GetMapping
    public ResponseEntity<PatternDto> getLatestPattern() {
        return playerPatternRepository
                .findTopByUsernameOrderByComputedAtDesc(appProperties.chessCom().username())
                .map(PatternDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
