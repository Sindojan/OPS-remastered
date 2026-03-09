package com.owlsburg.ops.common;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantModuleRepository extends JpaRepository<TenantModuleEntity, UUID> {

    List<TenantModuleEntity> findByTenantIdAndEnabledTrue(UUID tenantId);

    List<TenantModuleEntity> findByTenantId(UUID tenantId);

    Optional<TenantModuleEntity> findByTenantIdAndModuleId(UUID tenantId, String moduleId);
}
