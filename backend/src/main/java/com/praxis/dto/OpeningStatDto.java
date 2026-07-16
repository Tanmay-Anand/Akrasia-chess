package com.praxis.dto;

public record OpeningStatDto(
    String eco,
    String name,
    int games,
    int wins,
    double winPct
) {}
