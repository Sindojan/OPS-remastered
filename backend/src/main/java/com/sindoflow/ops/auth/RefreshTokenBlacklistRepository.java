package com.sindoflow.ops.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface RefreshTokenBlacklistRepository extends JpaRepository<RefreshTokenBlacklistEntity, UUID> {

    boolean existsByTokenHash(String tokenHash);

    void deleteByExpiredAtBefore(Instant before);
}
