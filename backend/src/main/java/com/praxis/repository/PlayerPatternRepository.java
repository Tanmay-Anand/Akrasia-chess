package com.praxis.repository;

import com.praxis.domain.PlayerPattern;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlayerPatternRepository extends JpaRepository<PlayerPattern, UUID> {

    Optional<PlayerPattern> findTopByUsernameOrderByComputedAtDesc(String username);
}
