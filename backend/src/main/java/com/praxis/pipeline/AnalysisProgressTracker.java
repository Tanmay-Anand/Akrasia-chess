package com.praxis.pipeline;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AnalysisProgressTracker {

    private volatile boolean running = false;
    private volatile boolean patternGenerating = false;
    private volatile boolean queued = false;
    private final AtomicInteger completed = new AtomicInteger(0);
    private volatile int total = 0;
    private volatile Instant startedAt = null;

    public void start(int totalGames) {
        this.queued = false;
        this.total = totalGames;
        this.completed.set(0);
        this.patternGenerating = false;
        this.startedAt = Instant.now();
        this.running = true;
    }

    public void setQueued(boolean v) { this.queued = v; }

    public void increment() {
        completed.incrementAndGet();
    }

    public void setPatternGenerating(boolean v) {
        this.patternGenerating = v;
    }

    public void finish() {
        this.running = false;
        this.patternGenerating = false;
    }

    public boolean isRunning() { return running; }
    public boolean isQueued() { return queued; }
    public boolean isPatternGenerating() { return patternGenerating; }
    public int getCompleted() { return completed.get(); }
    public int getTotal() { return total; }

    public long getEtaSeconds() {
        if (!running || startedAt == null || completed.get() == 0) return -1;
        long elapsed = Duration.between(startedAt, Instant.now()).getSeconds();
        if (elapsed == 0) return -1;
        double secsPerGame = (double) elapsed / completed.get();
        int remaining = Math.max(0, total - completed.get());
        return (long) (secsPerGame * remaining);
    }
}
