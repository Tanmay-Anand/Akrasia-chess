package com.praxis.service.chesscom;

import com.praxis.service.chesscom.dto.ChessComGame;
import com.praxis.service.chesscom.dto.ChessComGamesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class ChessComApiClient {

    private static final Logger log = LoggerFactory.getLogger(ChessComApiClient.class);
    private static final String BASE_URL = "https://api.chess.com/pub";
    private static final String USER_AGENT = "Praxis-Chess/1.0 (personal analytics tool)";

    private final RestClient restClient;

    public ChessComApiClient() {
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("User-Agent", USER_AGENT)
                .build();
    }

    public List<ChessComGame> fetchGames(String username, int year, int month) {
        String path = "/player/%s/games/%d/%02d".formatted(username, year, month);
        log.debug("Fetching Chess.com games: {}", path);

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                ChessComGamesResponse response = restClient.get()
                        .uri(path)
                        .retrieve()
                        .body(ChessComGamesResponse.class);

                if (response == null || response.games() == null) return List.of();
                return response.games();

            } catch (HttpClientErrorException e) {
                if (e.getStatusCode().value() == 429 && attempt < 3) {
                    long backoffMs = 10_000L * attempt;
                    log.warn("Chess.com rate limited (429) for {}/{}/{} — retrying in {}s (attempt {}/3)",
                            username, year, month, backoffMs / 1000, attempt + 1);
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return List.of();
                    }
                } else {
                    log.warn("Failed to fetch games for {}/{}/{}: {} {}", username, year, month,
                            e.getStatusCode(), e.getMessage());
                    return List.of();
                }
            } catch (Exception e) {
                log.warn("Failed to fetch games for {}/{}/{}: {}", username, year, month, e.getMessage());
                return List.of();
            }
        }
        return List.of();
    }
}
