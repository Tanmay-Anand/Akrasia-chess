package com.praxis.service.chesscom.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChessComGame(
    String uuid,
    String pgn,
    @JsonProperty("time_control") String timeControl,
    @JsonProperty("end_time") long endTime,
    @JsonProperty("time_class") String timeClass,
    ChessComPlayer white,
    ChessComPlayer black
) {}
