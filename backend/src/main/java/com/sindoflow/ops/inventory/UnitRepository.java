package com.sindoflow.ops.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UnitRepository extends JpaRepository<UnitEntity, UUID> {

    Optional<UnitEntity> findByName(String name);

    Optional<UnitEntity> findByAbbreviation(String abbreviation);

    boolean existsByName(String name);

    boolean existsByAbbreviation(String abbreviation);
}
