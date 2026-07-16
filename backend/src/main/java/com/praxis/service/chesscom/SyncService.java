package com.praxis.service.chesscom;

import com.praxis.config.AppProperties;
import com.praxis.domain.Game;
import com.praxis.domain.SyncHistory;
import com.praxis.domain.enums.AnalysisStatus;
import com.praxis.pipeline.AnalysisPipelineOrchestrator;
import com.praxis.pipeline.AnalysisProgressTracker;
import com.praxis.repository.GameRepository;
import com.praxis.repository.SyncHistoryRepository;
import com.praxis.service.chesscom.dto.ChessComGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    private final ChessComApiClient apiClient;
    private final GameRepository gameRepository;
    private final SyncHistoryRepository syncHistoryRepository;
    private final AnalysisPipelineOrchestrator pipelineOrchestrator;
    private final AnalysisProgressTracker progressTracker;
    private final AppProperties appProperties;

    // In-memory sync state
    private volatile boolean syncing = false;
    private volatile boolean syncQueued = false;
    private final AtomicInteger gamesFetched  = new AtomicInteger(0);
    private final AtomicInteger gamesQueued   = new AtomicInteger(0);
    private final AtomicReference<String> lastSyncedAt = new AtomicReference<>("Never");

    public SyncService(ChessComApiClient apiClient,
                       GameRepository gameRepository,
                       SyncHistoryRepository syncHistoryRepository,
                       AnalysisPipelineOrchestrator pipelineOrchestrator,
                       AnalysisProgressTracker progressTracker,
                       AppProperties appProperties) {
        this.apiClient = apiClient;
        this.gameRepository = gameRepository;
        this.syncHistoryRepository = syncHistoryRepository;
        this.pipelineOrchestrator = pipelineOrchestrator;
        this.progressTracker = progressTracker;
        this.appProperties = appProperties;
    }

    @Transactional
    public int sync(String username, int months) {
        if (syncing) {
            log.info("Sync already in progress, skipping");
            return 0;
        }

        String effectiveUsername = username != null ? username : appProperties.chessCom().username();
        syncQueued = false;
        syncing = true;
        gamesFetched.set(0);
        gamesQueued.set(0);

        try {
            List<Game> newGames = new ArrayList<>();
            YearMonth current = YearMonth.now();

            for (int i = 0; i < months; i++) {
                YearMonth ym = current.minusMonths(i);
                if (syncHistoryRepository.existsByUsernameAndYearAndMonth(effectiveUsername, ym.getYear(), ym.getMonthValue())) {
                    log.debug("Month {}/{} already synced, skipping", ym.getYear(), ym.getMonthValue());
                    continue;
                }

                List<ChessComGame> fetched = apiClient.fetchGames(effectiveUsername, ym.getYear(), ym.getMonthValue());
                int persisted = 0;

                for (ChessComGame cg : fetched) {
                    if (cg.uuid() == null) continue;
                    if (gameRepository.existsByChessComId(cg.uuid())) {
                        // Backfill accuracy for games synced before the accuracy field was added
                        gameRepository.findByChessComId(cg.uuid()).ifPresent(existing -> {
                            if (existing.getAccuracy() == null) {
                                boolean piw = effectiveUsername.equalsIgnoreCase(cg.white().username());
                                Double acc = piw ? cg.white().accuracy() : cg.black().accuracy();
                                if (acc != null) {
                                    existing.setAccuracy(acc);
                                    gameRepository.save(existing);
                                }
                            }
                        });
                        continue;
                    }
                    Game game = toEntity(cg, effectiveUsername);
                    gameRepository.save(game);
                    newGames.add(game);
                    persisted++;
                }

                SyncHistory history = SyncHistory.builder()
                        .username(effectiveUsername)
                        .year(ym.getYear())
                        .month(ym.getMonthValue())
                        .gamesFetched(persisted)
                        .build();
                syncHistoryRepository.save(history);

                gamesFetched.addAndGet(persisted);
                log.info("Synced {}/{}: {} new games", ym.getYear(), ym.getMonthValue(), persisted);
            }

            lastSyncedAt.set(OffsetDateTime.now(ZoneOffset.UTC).toString());
            gamesQueued.set(newGames.size());

            if (!newGames.isEmpty()) {
                progressTracker.setQueued(true);
                pipelineOrchestrator.analyzeGames(newGames, effectiveUsername);
            }

            return newGames.size();
        } finally {
            syncing = false;
        }
    }

    public SyncStatus getStatus() {
        String username = appProperties.chessCom().username();
        long pending  = gameRepository.countByUsernameAndAnalysisStatus(username, AnalysisStatus.PENDING);
        long analyzed = gameRepository.countByUsernameAndAnalysisStatus(username, AnalysisStatus.ANALYZED);
        // Always read from DB so it survives server restarts
        String lastSync = syncHistoryRepository
                .findTopByUsernameOrderBySyncedAtDesc(username)
                .map(sh -> sh.getSyncedAt().toString())
                .orElse("Never");
        return new SyncStatus((syncing || syncQueued) ? "SYNCING" : (pending > 0 ? "ANALYZING" : "IDLE"),
                gamesFetched.get(), (int) analyzed, (int) pending, lastSync);
    }

    public void enqueueSyncFlag() { this.syncQueued = true; }

    public void clearSyncHistory(String username, int months) {
        YearMonth current = YearMonth.now();
        for (int i = 0; i < months; i++) {
            YearMonth ym = current.minusMonths(i);
            syncHistoryRepository.deleteByUsernameAndYearAndMonth(username, ym.getYear(), ym.getMonthValue());
        }
    }

    public int forceResync(String username, int months) {
        String effectiveUsername = username != null ? username : appProperties.chessCom().username();
        clearSyncHistory(effectiveUsername, months);
        return sync(username, months);
    }

    private Game toEntity(ChessComGame cg, String username) {
        boolean playerIsWhite = username.equalsIgnoreCase(cg.white().username());
        String playerColor = playerIsWhite ? "white" : "black";
        String rawResult = playerIsWhite ? cg.white().result() : cg.black().result();
        String result = normalizeResult(rawResult);
        OffsetDateTime playedAt = OffsetDateTime.ofInstant(Instant.ofEpochSecond(cg.endTime()), ZoneOffset.UTC);

        Double accuracy = playerIsWhite ? cg.white().accuracy() : cg.black().accuracy();

        return Game.builder()
                .chessComId(cg.uuid())
                .username(username)
                .playedAt(playedAt)
                .timeControl(cg.timeControl())
                .timeClass(cg.timeClass())
                .playerColor(playerColor)
                .result(result)
                .rawPgn(cg.pgn())
                .whiteRating(cg.white().rating())
                .blackRating(cg.black().rating())
                .accuracy(accuracy)
                .analysisStatus(AnalysisStatus.PENDING)
                .build();
    }

    private String normalizeResult(String chessComResult) {
        if (chessComResult == null) return "draw";
        return switch (chessComResult) {
            case "win" -> "win";
            case "resigned", "checkmated", "timeout", "abandoned", "lose" -> "loss";
            default -> "draw";
        };
    }

    public record SyncStatus(String state, int gamesFetched, int gamesAnalyzed, int gamesPending, String lastSyncedAt) {}
}
