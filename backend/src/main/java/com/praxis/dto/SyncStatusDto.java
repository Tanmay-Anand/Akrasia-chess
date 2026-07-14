package com.praxis.dto;

public record SyncStatusDto(
    String state,
    int gamesFetched,
    int gamesAnalyzed,
    int gamesPending,
    String lastSyncedAt
) {}
