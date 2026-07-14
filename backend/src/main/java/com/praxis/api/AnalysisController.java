package com.praxis.api;

import com.praxis.dto.MoveErrorDto;
import com.praxis.repository.MoveErrorRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final MoveErrorRepository moveErrorRepository;

    public AnalysisController(MoveErrorRepository moveErrorRepository) {
        this.moveErrorRepository = moveErrorRepository;
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<List<MoveErrorDto>> getMoveErrors(@PathVariable UUID gameId) {
        List<MoveErrorDto> errors = moveErrorRepository.findByGameId(gameId)
                .stream()
                .map(MoveErrorDto::from)
                .toList();
        return ResponseEntity.ok(errors);
    }
}
