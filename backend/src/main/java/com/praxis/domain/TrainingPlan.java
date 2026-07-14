package com.praxis.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "training_plans")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(name = "generated_at", nullable = false)
    private OffsetDateTime generatedAt;

    @Column(name = "based_on_games", nullable = false)
    private int basedOnGames;

    // Full structured plan JSON from Ollama
    @Column(name = "plan_json", nullable = false, columnDefinition = "TEXT")
    private String planJson;

    // JSON array string: ["C60: Ruy Lopez", ...]
    @Column(name = "openings_to_drill", columnDefinition = "TEXT")
    private String openingsToDrill;

    // JSON array string: ["FORK", "PIN", ...]
    @Column(name = "tactical_patterns", columnDefinition = "TEXT")
    private String tacticalPatterns;
}
