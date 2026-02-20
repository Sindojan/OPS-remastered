package com.sindoflow.ops.tenant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<TenantEntity, UUID> {

    Optional<TenantEntity> findByTenantId(String tenantId);

    List<TenantEntity> findByActiveTrue();

    boolean existsByTenantId(String tenantId);
}
