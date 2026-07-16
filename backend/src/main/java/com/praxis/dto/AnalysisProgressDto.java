package com.praxis.dto;

public record AnalysisProgressDto(
    boolean running,
    boolean patternGenerating,
    boolean queued,
    int completed,
    int total,
    int percentComplete,
    long etaSeconds
) {}
