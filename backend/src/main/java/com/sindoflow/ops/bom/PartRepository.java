package com.sindoflow.ops.bom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PartRepository extends JpaRepository<PartEntity, UUID> {

    Optional<PartEntity> findByPartNumber(String partNumber);

    boolean existsByPartNumber(String partNumber);
}
