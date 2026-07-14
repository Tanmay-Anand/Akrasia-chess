package com.praxis.dto;

public record SyncRequestDto(String username, Integer months) {
    public int effectiveMonths() { return months != null ? months : 1; }
}
