package com.praxis.service.analysis;

import com.praxis.config.AppProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;

@Component
public class StockfishService {

    private static final Logger log = LoggerFactory.getLogger(StockfishService.class);

    private final AppProperties props;
    private volatile Process process;
    private BufferedWriter writer;
    private BufferedReader reader;

    public StockfishService(AppProperties props) {
        this.props = props;
    }

    @PostConstruct
    public void init() {
        String path = props.stockfishPath();
        if (path.isBlank()) {
            log.info("Stockfish not configured — set praxis-chess.stockfish.path in application.yml");
            return;
        }
        try {
            process = new ProcessBuilder(path)
                    .redirectErrorStream(true)
                    .start();
            writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
            reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

            send("uci");
            waitFor("uciok");
            send("setoption name Threads value 2");
            send("setoption name Hash value 64");
            send("isready");
            waitFor("readyok");
            log.info("Stockfish ready: {}", path);
        } catch (Exception e) {
            log.warn("Stockfish failed to start ({}): {} — using material fallback", path, e.getMessage());
            process = null;
        }
    }

    @PreDestroy
    public void destroy() {
        if (process != null && process.isAlive()) {
            try { send("quit"); } catch (Exception ignored) {}
            process.destroyForcibly();
        }
    }

    public boolean isAvailable() {
        return process != null && process.isAlive();
    }

    // Returns evaluation in pawns from White's perspective. null on failure.
    public synchronized Double evaluate(String fen) {
        if (!isAvailable()) return null;
        try {
            send("position fen " + fen);
            send("go movetime 50");

            Double score = null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("bestmove")) break;
                if (line.startsWith("info") && line.contains(" score ")) {
                    Double s = parseScore(line);
                    if (s != null) score = s;
                }
            }
            return score;
        } catch (IOException e) {
            log.warn("Stockfish evaluation error: {}", e.getMessage());
            return null;
        }
    }

    private Double parseScore(String infoLine) {
        // centipawn score: "score cp 45"
        int cpIdx = infoLine.indexOf(" score cp ");
        if (cpIdx >= 0) {
            int start = cpIdx + 10;
            int end = infoLine.indexOf(' ', start);
            String val = (end >= 0 ? infoLine.substring(start, end) : infoLine.substring(start)).trim();
            try { return Integer.parseInt(val) / 100.0; } catch (NumberFormatException ignored) {}
        }
        // mate score: "score mate 3" (positive = White mates), "score mate -2" (negative = Black mates)
        int mateIdx = infoLine.indexOf(" score mate ");
        if (mateIdx >= 0) {
            int start = mateIdx + 12;
            int end = infoLine.indexOf(' ', start);
            String val = (end >= 0 ? infoLine.substring(start, end) : infoLine.substring(start)).trim();
            try {
                int m = Integer.parseInt(val);
                return m > 0 ? 100.0 : -100.0;
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private void send(String cmd) throws IOException {
        writer.write(cmd);
        writer.newLine();
        writer.flush();
    }

    private void waitFor(String token) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains(token)) return;
        }
    }
}
