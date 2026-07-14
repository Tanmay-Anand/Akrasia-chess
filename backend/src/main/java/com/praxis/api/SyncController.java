package com.praxis.api;

import com.praxis.dto.SyncRequestDto;
import com.praxis.dto.SyncStatusDto;
import com.praxis.service.chesscom.SyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> triggerSync(@RequestBody(required = false) SyncRequestDto request) {
        int months = request != null ? request.effectiveMonths() : 1;
        String username = request != null ? request.username() : null;
        int newGames = syncService.sync(username, months);
        return ResponseEntity.ok(Map.of("message", "Sync started", "newGames", newGames));
    }

    @GetMapping("/status")
    public ResponseEntity<SyncStatusDto> getStatus() {
        SyncService.SyncStatus status = syncService.getStatus();
        return ResponseEntity.ok(new SyncStatusDto(
                status.state(), status.gamesFetched(),
                status.gamesAnalyzed(), status.gamesPending(),
                status.lastSyncedAt()));
    }
}
