package com.praxis.repository;

import com.praxis.domain.SyncHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SyncHistoryRepository extends JpaRepository<SyncHistory, UUID> {

    boolean existsByUsernameAndYearAndMonth(String username, int year, int month);

    Optional<SyncHistory> findByUsernameAndYearAndMonth(String username, int year, int month);
}
