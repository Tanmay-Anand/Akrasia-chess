package com.praxis.repository;

import com.praxis.domain.MoveError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface MoveErrorRepository extends JpaRepository<MoveError, UUID> {

    List<MoveError> findByGameId(UUID gameId);

    @Query("""
        SELECT me FROM MoveError me
        JOIN me.game g
        WHERE g.username = :username
        """)
    List<MoveError> findAllByUsername(String username);

    @Query("""
        SELECT me FROM MoveError me
        JOIN me.game g
        WHERE g.username = :username AND me.analysisFailed = false
        """)
    List<MoveError> findSuccessfulByUsername(String username);

    void deleteByGameId(UUID gameId);
}
