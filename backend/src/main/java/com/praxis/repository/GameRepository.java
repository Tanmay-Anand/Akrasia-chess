package com.praxis.repository;

import com.praxis.domain.Game;
import com.praxis.domain.enums.AnalysisStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameRepository extends JpaRepository<Game, UUID> {

    boolean existsByChessComId(String chessComId);

    List<Game> findByUsernameOrderByPlayedAtDesc(String username);

    List<Game> findByUsernameAndAnalysisStatus(String username, AnalysisStatus status);

    long countByUsernameAndAnalysisStatus(String username, AnalysisStatus status);

    @Query("SELECT g FROM Game g WHERE g.username = :username ORDER BY g.playedAt DESC")
    List<Game> findRecentByUsername(String username);

    Optional<Game> findByChessComId(String chessComId);
}
