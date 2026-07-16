package com.praxis.repository;

import com.praxis.domain.SyncHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface SyncHistoryRepository extends JpaRepository<SyncHistory, UUID> {

    boolean existsByUsernameAndYearAndMonth(String username, int year, int month);

    Optional<SyncHistory> findByUsernameAndYearAndMonth(String username, int year, int month);

    Optional<SyncHistory> findTopByUsernameOrderBySyncedAtDesc(String username);

    @Transactional
    @Modifying
    void deleteByUsernameAndYearAndMonth(String username, int year, int month);
}
