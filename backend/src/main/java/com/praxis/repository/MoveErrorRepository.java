package com.praxis.repository;

import com.praxis.domain.MoveError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    @Modifying
    void deleteByGameId(UUID gameId);

    @Query("""
        SELECT COUNT(me) FROM MoveError me
        JOIN me.game g
        WHERE g.username = :username AND me.severity = 'BLUNDER' AND me.analysisFailed = false
        """)
    int countBlundersByUsername(String username);

    @Query("""
        SELECT me.game.id, COUNT(me) FROM MoveError me
        JOIN me.game g
        WHERE g.username = :username AND me.analysisFailed = false
        GROUP BY me.game.id
        """)
    List<Object[]> countSuccessfulPerGameByUsername(@Param("username") String username);
}
