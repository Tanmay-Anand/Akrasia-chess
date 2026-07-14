# ♟ Akrasia

A personal, fully offline chess analytics and improvement system. Connect your Chess.com account, let the AI analyze your games, identify your recurring weaknesses, and generate a training plan tailored to your actual play — not generic advice.

Built with **Java 21 + Spring Boot**, **PostgreSQL**, **Ollama (local LLM)**, and **React**. No cloud AI. No deployment. Runs entirely on your machine.

---

## What It Does

### Dashboard
Your chess at a glance. Rating trend over time, win/loss/draw breakdown, opening distribution across your recent games, and accuracy trends — all derived from your Chess.com data.

### Tab 1 — Game Analysis
Select any of your fetched games and get move-by-move AI coaching. Every mistake is flagged with its severity (blunder, inaccuracy, missed opportunity), the better move, and a plain-English explanation of *why* — not just what.

### Tab 2 — Pattern Report
Cross-game aggregation. Instead of per-game feedback, this tells you what *keeps* happening: which move range you blunder in most, which tactical motifs catch you repeatedly, and where your opening preparation breaks down.

### Tab 3 — Training Plan
The AI reads your Tab 1 + Tab 2 findings and generates a prioritized, concrete improvement list: specific openings to drill, tactical patterns to study, and positional habits to fix. Regenerates every time you sync new games.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3 |
| AI / LLM | Ollama (`qwen2.5:14b` Q4_K_M) |
| Database | PostgreSQL |
| PGN Parsing | `bhlangonijr/chess-library` |
| Data Source | Chess.com Public API (free, no key needed) |
| Frontend | React + TypeScript |
| Charts | Recharts |
| Build | Maven |

---

## Prerequisites

- Java 21+
- PostgreSQL 15+
- [Ollama](https://ollama.com) installed and running
- Node.js 18+ (frontend)

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/Tanmay-Anand/akrasia.git
cd akrasia
```

### 2. Pull the LLM model

```bash
ollama pull qwen2.5:14b
```

> If your GPU struggles, try `ollama pull llama3.2:3b` as a lighter fallback.

### 3. Set up PostgreSQL

```sql
CREATE DATABASE akrasia;
CREATE USER akrasia_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE akrasia TO akrasia_user;
```

### 4. Configure the application

Copy the example config and fill in your values:

```bash
cp src/main/resources/application.example.yml src/main/resources/application.yml
```

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/akrasia
    username: akrasia_user
    password: your_password

akrasia:
  ollama:
    base-url: http://localhost:11434
    model: qwen2.5:14b
  chess-com:
    username: your_chess_com_username
```

### 5. Run the backend

```bash
./mvnw spring-boot:run
```

### 6. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173` in your browser.

---

## How Analysis Works

The AI layer uses a **structured output pipeline** — Ollama never returns a wall of text. Every analysis call returns a typed JSON schema that the backend processes deterministically:

```json
{
  "mistakes": [
    {
      "move_number": 14,
      "move_played": "Nf6??",
      "better_move": "d5",
      "severity": "BLUNDER",
      "explanation": "Allows Bxf7+ winning the exchange immediately. d5 instead contests the center and maintains equality."
    }
  ],
  "opening_deviation_at_move": 7,
  "patterns_detected": ["BACK_RANK_WEAKNESS", "PREMATURE_ATTACK"],
  "phase_weakness": "MIDDLEGAME"
}
```

This means results are consistent, parseable, and storable — not dependent on how the LLM decides to format its response on a given day.

---

## Project Structure

```
akrasia/
├── src/
│   └── main/
│       ├── java/com/akrasia/
│       │   ├── api/              # REST controllers
│       │   ├── domain/           # Domain models and enums
│       │   ├── service/
│       │   │   ├── analysis/     # PGN parsing, position evaluation
│       │   │   ├── ai/           # Ollama orchestration, prompt templates
│       │   │   └── chesscom/  # Chess.com API client
│       │   ├── repository/       # JPA repositories
│       │   └── pipeline/         # Agentic analysis pipeline
│       └── resources/
│           ├── application.yml
│           └── db/migration/     # Flyway migrations
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   └── hooks/
│   └── package.json
└── README.md
```

---

## Roadmap

- [ ] Chess.com API integration + game sync
- [ ] PGN parsing pipeline
- [ ] Structured Ollama analysis engine
- [ ] Game Analysis tab (Tab 1)
- [ ] Pattern aggregation engine (Tab 2)
- [ ] Training plan generator (Tab 3)
- [ ] Dashboard with charts
- [ ] Opening drill integration (link to Chess Opening Trainer)
- [ ] Fine-tuned model on Lichess annotated game dataset

---

## Why Fully Offline?

Your game history is personal. This platform intentionally sends nothing to external AI services. The Chess.com Public API is the only outbound call — and that's read-only public data you already own.

---

## License

MIT
