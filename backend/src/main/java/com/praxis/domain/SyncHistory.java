package com.praxis.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sync_history", uniqueConstraints = {
    @UniqueConstraint(name = "uq_sync_history_username_year_month",
        columnNames = {"username", "year", "month"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private int month;

    @Column(name = "games_fetched")
    private int gamesFetched;

    @CreationTimestamp
    @Column(name = "synced_at", updatable = false)
    private OffsetDateTime syncedAt;
}
