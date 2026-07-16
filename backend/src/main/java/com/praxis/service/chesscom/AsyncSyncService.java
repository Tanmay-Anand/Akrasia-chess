package com.praxis.service.chesscom;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncSyncService {

    private final SyncService syncService;

    public AsyncSyncService(SyncService syncService) {
        this.syncService = syncService;
    }

    @Async("analysisExecutor")
    public void syncAsync(String username, int months) {
        syncService.sync(username, months);
    }

    @Async("analysisExecutor")
    public void forceResyncAsync(String username, int months) {
        syncService.forceResync(username, months);
    }
}
