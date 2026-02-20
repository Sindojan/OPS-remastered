package com.sindoflow.ops.bom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BomVersionRepository extends JpaRepository<BomVersionEntity, UUID> {

    List<BomVersionEntity> findByPartId(UUID partId);

    Optional<BomVersionEntity> findByPartIdAndStatus(UUID partId, VersionStatus status);

    List<BomVersionEntity> findByPartIdAndStatusNot(UUID partId, VersionStatus status);
}
