package com.praxis.api;

import com.praxis.dto.SyncRequestDto;
import com.praxis.dto.SyncStatusDto;
import com.praxis.service.chesscom.AsyncSyncService;
import com.praxis.service.chesscom.SyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final SyncService syncService;
    private final AsyncSyncService asyncSyncService;

    public SyncController(SyncService syncService, AsyncSyncService asyncSyncService) {
        this.syncService = syncService;
        this.asyncSyncService = asyncSyncService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> triggerSync(@RequestBody(required = false) SyncRequestDto request) {
        int months = request != null ? request.effectiveMonths() : 1;
        String username = request != null ? request.username() : null;
        syncService.enqueueSyncFlag();
        asyncSyncService.syncAsync(username, months);
        return ResponseEntity.accepted().body(Map.of("message", "Sync queued"));
    }

    @GetMapping("/status")
    public ResponseEntity<SyncStatusDto> getStatus() {
        SyncService.SyncStatus status = syncService.getStatus();
        return ResponseEntity.ok(new SyncStatusDto(
                status.state(), status.gamesFetched(),
                status.gamesAnalyzed(), status.gamesPending(),
                status.lastSyncedAt()));
    }

    @PostMapping("/force-resync")
    public ResponseEntity<Map<String, Object>> forceResync(@RequestBody(required = false) SyncRequestDto request) {
        int months = request != null ? request.effectiveMonths() : 3;
        String username = request != null ? request.username() : null;
        syncService.enqueueSyncFlag();
        asyncSyncService.forceResyncAsync(username, months);
        return ResponseEntity.accepted().body(Map.of("message", "Re-sync queued"));
    }
}
