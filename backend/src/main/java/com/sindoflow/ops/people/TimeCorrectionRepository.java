package com.sindoflow.ops.people;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TimeCorrectionRepository extends JpaRepository<TimeCorrectionEntity, UUID> {

    List<TimeCorrectionEntity> findByTimeEntryId(UUID timeEntryId);
}
