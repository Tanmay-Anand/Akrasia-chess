# Architecture Design — Praxis-Chess

**Status:** Draft v1
**Stack:** Java 21 · Spring Boot 3 · PostgreSQL · Ollama · React + TypeScript
**Scope:** Local-only personal chess analytics platform. No cloud deployment. No external AI services.

---

## 1. Design Principles

1. **Structured outputs over free text.** The LLM never returns a wall of prose. Every Ollama call is constrained to a typed JSON schema. Deterministic Java code processes the result — not string parsing.

2. **The LLM analyzes; it does not orchestrate.** The agentic pipeline is a Java state machine. The LLM is called at defined nodes with a specific prompt and a specific expected output shape. It does not decide what to do next.

3. **Offline by design.** Ollama runs locally. PostgreSQL runs locally. The only outbound network call is to the Chess.com Public API (read-only, no auth, public data).

4. **Aggregate, don't just report.** Single-game analysis is table stakes. The real value is cross-game pattern detection — what keeps happening across all your games.

5. **Persistence-first.** Every analyzed game, detected pattern, and generated training plan is stored in PostgreSQL. Re-analysis is cheap; re-fetching and re-parsing is not.

---

## 2. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        React Frontend                        │
│         Dashboard · Game Analysis · Patterns · Plan          │
└───────────────────────────┬─────────────────────────────────┘
                            │ HTTP / REST
┌───────────────────────────▼─────────────────────────────────┐
│                   Spring Boot API Layer                       │
│              Controllers · DTOs · Validation                  │
└──────┬────────────────────┬───────────────────┬─────────────┘
       │                    │                   │
┌──────▼──────┐   ┌─────────▼──────┐   ┌───────▼──────────┐
│  Chess.com  │   │  Analysis      │   │  Training Plan   │
│  Sync Svc   │   │  Pipeline      │   │  Generator       │
│             │   │  (State Machine│   │                  │
│  Fetch PGNs │   │   + Ollama)    │   │  Reads patterns  │
└──────┬──────┘   └─────────┬──────┘   │  → Ollama →      │
       │                    │          │  structured plan  │
┌──────▼────────────────────▼──────────▼──────────────────┐
│                      Domain Services                      │
│       PGN Parser · Position Evaluator · Aggregator        │
└──────────────────────────┬────────────────────────────────┘
                           │
              ┌────────────▼────────────┐
              │       PostgreSQL         │
              │  games · moves · errors  │
              │  patterns · plans · sync │
              └─────────────────────────┘
                           │
              ┌────────────▼────────────┐
              │    Ollama (local)        │
              │    qwen2.5:14b Q4_K_M   │
              │    http://localhost:11434│
              └─────────────────────────┘
```

---

## 3. Module Breakdown

### 3.1 Chess.com Sync Service

Responsible for fetching game data from the Chess.com Public API and persisting raw PGN + metadata.

**Endpoints used:**

```
GET https://api.chess.com/pub/player/{username}/games/{year}/{month}
GET https://api.chess.com/pub/player/{username}/stats
```

**Flow:**

```
SyncRequest(username, months)
  → ChessComApiClient.fetchGames()
  → Filter already-synced games (check game_id in DB)
  → Persist raw PGN + metadata to games table
  → Trigger analysis pipeline (async)
```

**Key design decisions:**

- Chess.com API requires no key. Include a `User-Agent` header to avoid rate limiting.
- Cache-aware: store `last_synced_at` per month. Skip re-fetching months already fully synced.
- Serial requests only — the API rate-limits parallel calls with 429.

---

### 3.2 PGN Parsing Pipeline

Converts raw PGN strings into structured move sequences the analysis engine can work with.

**Library:** `bhlangonijr/chess-library` (pure Java, no native deps)

**Output per game:**

```java
record ParsedGame(
    String gameId,
    String opening,           // ECO code + name from PGN headers
    int totalMoves,
    String result,            // "1-0" | "0-1" | "1/2-1/2"
    String playerColor,       // "white" | "black"
    List<ParsedMove> moves    // FEN before move, SAN notation, time remaining
)
```

**Output per move:**

```java
record ParsedMove(
    int moveNumber,
    String san,               // Standard Algebraic Notation e.g. "Nf3"
    String fenBefore,         // Board position before this move
    String fenAfter,
    int clockRemainingSeconds // from %clk annotation if present
)
```

---

### 3.3 Analysis Pipeline (Core — Agentic State Machine)

This is the heart of the platform. A deterministic Java state machine that orchestrates PGN parsing, position evaluation, and Ollama analysis calls.

```
              ┌─────────────────┐
              │   PARSE_PGN     │
              └────────┬────────┘
                       │
              ┌────────▼────────┐
              │  EVALUATE_      │
              │  POSITIONS      │  ← score each position (material count,
              └────────┬────────┘    basic heuristics — no Stockfish needed
                       │             for the MVP)
              ┌────────▼────────┐
              │  IDENTIFY_      │
              │  CANDIDATE_     │  ← flag moves with large evaluation swings
              │  MISTAKES       │    as candidates for LLM analysis
              └────────┬────────┘
                       │
              ┌────────▼────────┐
              │  OLLAMA_        │
              │  ANALYZE        │  ← send candidate positions to Ollama
              └────────┬────────┘    with structured output schema
                       │
              ┌────────▼────────┐
              │  PERSIST_       │
              │  RESULTS        │  ← store structured results to DB
              └────────┬────────┘
                       │
              ┌────────▼────────┐
              │  UPDATE_        │
              │  PATTERNS       │  ← update cross-game pattern aggregates
              └─────────────────┘
```

**Why candidate pre-filtering before Ollama?**
Sending every move to the LLM is slow and wastes context. A simple Java heuristic (material swing > 1.5 pawns, or time pressure + blunder) identifies the 3–8 most critical moments per game. Only those go to Ollama.

---

### 3.4 Ollama Integration — Structured Output Contract

Ollama is never asked open-ended questions. Every call uses a prompt that demands a specific JSON shape and nothing else.

**Game Analysis Prompt Template:**

```
You are a chess coach analyzing a specific position.

Position (FEN): {fen_before}
Move played: {san_move}
Player color: {player_color}
Game phase: {phase}  (OPENING | MIDDLEGAME | ENDGAME)
Move number: {move_number}

Analyze why this move is a mistake and what the better alternative was.

Respond ONLY with a JSON object matching this exact schema. No preamble. No explanation outside the JSON:
{
  "severity": "BLUNDER" | "MISTAKE" | "INACCURACY",
  "better_move": "<SAN notation>",
  "explanation": "<2-3 sentence plain English explanation of why the played move is bad and why the better move is stronger>",
  "tactical_motif": "<one of: FORK | PIN | SKEWER | BACK_RANK | DISCOVERED_ATTACK | HANGING_PIECE | POSITIONAL | OTHER>",
  "phase_assessment": "<one sentence about the position's phase-specific demands>"
}
```

**Pattern Report Prompt Template:**

```
You are a chess coach reviewing aggregated mistake data for a player.

Games analyzed: {game_count}
Total mistakes flagged: {total_mistakes}

Mistake breakdown by move range:
{move_range_distribution}

Tactical motif frequency:
{motif_frequency_map}

Opening deviation summary:
{opening_deviation_data}

Identify the player's top 3 recurring weaknesses. Respond ONLY with this JSON schema:
{
  "primary_weakness": "<one sentence>",
  "secondary_weakness": "<one sentence>",
  "tertiary_weakness": "<one sentence>",
  "critical_move_range": "<e.g. moves 10-20>",
  "dominant_motif": "<motif name>",
  "opening_assessment": "<one sentence about opening accuracy>"
}
```

**Training Plan Prompt Template:**

```
You are a chess coach generating a prioritized improvement plan.

Player weaknesses identified:
{pattern_report_json}

Opening performance:
{opening_stats_json}

Generate a concrete training plan. Respond ONLY with this JSON schema:
{
  "priority_1": {
    "focus": "<what to work on>",
    "action": "<specific drill or study task>",
    "reason": "<why this is the top priority>"
  },
  "priority_2": { ... },
  "priority_3": { ... },
  "openings_to_drill": ["<ECO code: name>", ...],
  "tactical_patterns_to_study": ["<pattern name>", ...]
}
```

**Ollama call implementation:**

````java
@Service
public class OllamaAnalysisClient {

    private final RestClient restClient;

    public <T> T analyze(String prompt, Class<T> responseType) {
        String raw = restClient.post()
            .uri("/api/generate")
            .body(Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false,
                "format", "json"   // Ollama's JSON mode — enforces valid JSON output
            ))
            .retrieve()
            .body(OllamaResponse.class)
            .response();

        // Strip any accidental markdown fences before parsing
        String clean = raw.replaceAll("```json|```", "").trim();
        return objectMapper.readValue(clean, responseType);
    }
}
````

---

### 3.5 Pattern Aggregation Engine

Runs after every new batch of games is analyzed. Reads from persisted move errors and builds aggregated statistics stored in the `player_patterns` table.

**What it aggregates:**

- Mistake frequency by move range (1–10, 11–20, 21–30, 31+)
- Tactical motif frequency across all games
- Opening accuracy per ECO code (% of games where theory was followed through move N)
- Game phase where most material is lost (OPENING / MIDDLEGAME / ENDGAME)
- Time pressure correlation (mistakes when clock < 30s)

These aggregates are what feed the Pattern Report Ollama call — pre-computed, not re-derived on every request.

---

## 4. Database Schema

```sql
-- Raw game data from Chess.com
CREATE TABLE games (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chess_com_id    VARCHAR(64) UNIQUE NOT NULL,   -- Chess.com's game ID
    username        VARCHAR(64) NOT NULL,
    played_at       TIMESTAMPTZ NOT NULL,
    time_control    VARCHAR(32),                    -- e.g. "600+5"
    time_class      VARCHAR(16),                    -- "rapid" | "blitz" | "bullet"
    player_color    VARCHAR(8) NOT NULL,            -- "white" | "black"
    result          VARCHAR(8) NOT NULL,            -- "win" | "loss" | "draw"
    opening_eco     VARCHAR(8),                     -- e.g. "C60"
    opening_name    VARCHAR(128),
    raw_pgn         TEXT NOT NULL,
    analysis_status VARCHAR(16) DEFAULT 'PENDING',  -- PENDING | ANALYZED | FAILED
    analyzed_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- Individual move errors identified per game
CREATE TABLE move_errors (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id         UUID REFERENCES games(id) ON DELETE CASCADE,
    move_number     INT NOT NULL,
    player_color    VARCHAR(8) NOT NULL,
    move_played     VARCHAR(16) NOT NULL,           -- SAN notation
    better_move     VARCHAR(16),
    fen_position    TEXT NOT NULL,
    severity        VARCHAR(16) NOT NULL,           -- BLUNDER | MISTAKE | INACCURACY
    tactical_motif  VARCHAR(32),
    explanation     TEXT,
    game_phase      VARCHAR(16),                    -- OPENING | MIDDLEGAME | ENDGAME
    clock_remaining INT,                            -- seconds remaining when move played
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- Aggregated cross-game pattern data
CREATE TABLE player_patterns (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username                VARCHAR(64) NOT NULL,
    games_analyzed          INT NOT NULL,
    computed_at             TIMESTAMPTZ NOT NULL,

    -- Mistake distribution by move range
    mistakes_moves_1_10     INT DEFAULT 0,
    mistakes_moves_11_20    INT DEFAULT 0,
    mistakes_moves_21_30    INT DEFAULT 0,
    mistakes_moves_31_plus  INT DEFAULT 0,

    -- Phase breakdown
    mistakes_opening        INT DEFAULT 0,
    mistakes_middlegame     INT DEFAULT 0,
    mistakes_endgame        INT DEFAULT 0,

    -- Motif frequency (stored as JSONB for flexibility)
    motif_frequency         JSONB,   -- {"FORK": 12, "PIN": 8, "BACK_RANK": 5, ...}
    opening_accuracy        JSONB,   -- {"C60": {"games": 5, "avg_deviation_move": 7}, ...}

    -- LLM-generated summary fields
    primary_weakness        TEXT,
    secondary_weakness      TEXT,
    tertiary_weakness       TEXT,
    critical_move_range     VARCHAR(32),
    dominant_motif          VARCHAR(32),
    opening_assessment      TEXT
);

-- AI-generated training plans
CREATE TABLE training_plans (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username        VARCHAR(64) NOT NULL,
    generated_at    TIMESTAMPTZ NOT NULL,
    based_on_games  INT NOT NULL,                   -- how many games this was derived from
    plan_json       JSONB NOT NULL,                 -- full structured plan from Ollama
    openings_to_drill       TEXT[],
    tactical_patterns       TEXT[]
);

-- Sync tracking — know what's already been fetched
CREATE TABLE sync_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username        VARCHAR(64) NOT NULL,
    year            INT NOT NULL,
    month           INT NOT NULL,
    games_fetched   INT,
    synced_at       TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(username, year, month)
);

-- Indexes
CREATE INDEX idx_games_username ON games(username);
CREATE INDEX idx_games_analysis_status ON games(analysis_status);
CREATE INDEX idx_move_errors_game_id ON move_errors(game_id);
CREATE INDEX idx_move_errors_severity ON move_errors(severity);
CREATE INDEX idx_move_errors_motif ON move_errors(tactical_motif);
CREATE INDEX idx_player_patterns_username ON player_patterns(username);
```

---

## 5. REST API Contract

### Sync

| Method | Endpoint           | Description                                          |
| ------ | ------------------ | ---------------------------------------------------- |
| `POST` | `/api/sync`        | Fetch latest games from Chess.com and queue analysis |
| `GET`  | `/api/sync/status` | Returns sync progress and analysis queue state       |

### Games

| Method | Endpoint                  | Description                                   |
| ------ | ------------------------- | --------------------------------------------- |
| `GET`  | `/api/games`              | List all synced games with analysis status    |
| `GET`  | `/api/games/{id}`         | Full game detail including all analyzed moves |
| `POST` | `/api/games/{id}/analyze` | Re-trigger analysis for a specific game       |

### Dashboard

| Method | Endpoint                        | Description                                                |
| ------ | ------------------------------- | ---------------------------------------------------------- |
| `GET`  | `/api/dashboard/stats`          | Win/loss/draw counts, accuracy trend, opening distribution |
| `GET`  | `/api/dashboard/rating-history` | Rating over time (from Chess.com headers)                  |

### Analysis

| Method | Endpoint                      | Description                                          |
| ------ | ----------------------------- | ---------------------------------------------------- |
| `GET`  | `/api/analysis/{gameId}`      | All move errors for a game with explanations         |
| `GET`  | `/api/patterns`               | Latest cross-game pattern report                     |
| `GET`  | `/api/training-plan`          | Most recent generated training plan                  |
| `POST` | `/api/training-plan/generate` | Generate a fresh training plan from current patterns |

---

## 6. Frontend Structure

```
frontend/src/
├── pages/
│   ├── Dashboard.tsx           # Rating graph + summary stats
│   ├── GameList.tsx            # Synced games with filter/sort
│   ├── GameAnalysis.tsx        # Tab 1 — move-by-move analysis
│   ├── PatternReport.tsx       # Tab 2 — cross-game patterns
│   └── TrainingPlan.tsx        # Tab 3 — AI training plan
├── components/
│   ├── ChessBoard.tsx          # Interactive board (chessboard.js wrapper)
│   ├── MoveErrorCard.tsx       # Single mistake with severity + explanation
│   ├── RatingChart.tsx         # Recharts line chart
│   ├── OpeningDistribution.tsx # Bar chart of openings played
│   ├── PatternHeatmap.tsx      # Mistake frequency by move range
│   └── SyncStatusBanner.tsx    # Live sync progress indicator
├── hooks/
│   ├── useGameAnalysis.ts
│   ├── usePatternReport.ts
│   └── useSyncStatus.ts
└── api/
    └── client.ts               # Typed API client (fetch wrapper)
```

**State management:** React Query (TanStack Query) for server state. No global state manager needed — all state lives in the server (PostgreSQL) or in component-local state.

---

## 7. Agentic Pipeline — Sequence Diagram

```
User clicks "Sync"
       │
       ▼
SyncController.triggerSync(username)
       │
       ▼
ChessComApiClient
  → GET /pub/player/{username}/games/{year}/{month}
  → Filter out game_ids already in DB
  → Persist new raw PGNs (status: PENDING)
       │
       ▼
AnalysisPipelineOrchestrator (async, per game)
  ┌────────────────────────────────────────────┐
  │  STATE: PARSE_PGN                          │
  │  PgnParserService.parse(rawPgn)            │
  │  → List<ParsedMove>                        │
  └─────────────────────┬──────────────────────┘
                        │
  ┌─────────────────────▼──────────────────────┐
  │  STATE: EVALUATE_POSITIONS                 │
  │  PositionEvaluator.scoreAll(moves)         │
  │  → material balance per move               │
  └─────────────────────┬──────────────────────┘
                        │
  ┌─────────────────────▼──────────────────────┐
  │  STATE: IDENTIFY_CANDIDATE_MISTAKES        │
  │  MistakeCandidateFilter.filter(moves,      │
  │    scores, playerColor)                    │
  │  → List<CandidateMove> (max 8 per game)    │
  └─────────────────────┬──────────────────────┘
                        │
  ┌─────────────────────▼──────────────────────┐
  │  STATE: OLLAMA_ANALYZE                     │
  │  For each candidate:                       │
  │    OllamaAnalysisClient.analyze(           │
  │      buildPrompt(candidate),               │
  │      MoveAnalysisResult.class              │
  │    )                                       │
  │  → List<MoveAnalysisResult>                │
  └─────────────────────┬──────────────────────┘
                        │
  ┌─────────────────────▼──────────────────────┐
  │  STATE: PERSIST_RESULTS                    │
  │  MoveErrorRepository.saveAll(results)      │
  │  games.analysis_status = ANALYZED          │
  └─────────────────────┬──────────────────────┘
                        │
  ┌─────────────────────▼──────────────────────┐
  │  STATE: UPDATE_PATTERNS                    │
  │  PatternAggregator.recompute(username)     │
  │  → Upsert player_patterns row             │
  └────────────────────────────────────────────┘
```

---

## 8. Non-Functional Considerations

### Performance

- Ollama analysis is the bottleneck. With `qwen2.5:14b` on the RTX 3050, expect ~8–15 seconds per game (analyzing 5–8 candidate positions). Run analysis asynchronously — the UI polls `/api/sync/status` and updates progressively.
- PostgreSQL queries are fast for personal-scale data (hundreds of games). No query optimization needed until you have 10,000+ games.

### Reliability

- If Ollama returns malformed JSON, fall back to a repair attempt (strip fences, re-parse). If it still fails, mark that move error as `ANALYSIS_FAILED` and continue — don't block the whole game.
- Chess.com API returns 304 Not Modified when data hasn't changed — respect this and skip re-processing.

### Privacy

- No data leaves the machine except Chess.com API reads (public data).
- PostgreSQL runs locally. Ollama runs locally. React dev server runs locally.

---

## 9. Future Considerations

These are intentionally deferred — building them now adds complexity before the core loop is proven.

- **Stockfish integration** — real engine evaluation instead of material heuristics for candidate move detection. Higher accuracy, more complex setup.
- **Fine-tuned model** — train a small model (Qwen 1.5B) on Lichess annotated game dataset via Unsloth. Replace the generic `qwen2.5:14b` with a chess-specialist model.
- **Opening Trainer integration** — link detected opening weaknesses directly into the Chess Opening Trainer app's drill queue.
- **Opponent analysis** — "what does this player keep exploiting against you?"
- **Time pressure analysis** — deeper correlation between clock time and mistake rate.

---

_Schema migrations managed via ddl auto: update. Flyway will not be used here_
