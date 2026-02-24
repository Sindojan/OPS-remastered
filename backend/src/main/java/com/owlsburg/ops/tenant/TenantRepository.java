package com.owlsburg.ops.tenant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<TenantEntity, UUID> {
    List<TenantEntity> findByActiveTrue();
    Optional<TenantEntity> findBySlug(String slug);
    boolean existsBySlug(String slug);
    long countByStatus(String status);
    long countByCreatedAtAfter(Instant after);
}
