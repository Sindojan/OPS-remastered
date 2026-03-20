package com.owlsburg.ops.odoo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OdooConfigRepository extends JpaRepository<OdooConfigEntity, UUID> {

    Optional<OdooConfigEntity> findByTenantId(UUID tenantId);
}
