package com.owlsburg.ops.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface KnowledgeCategoryRepository extends JpaRepository<KnowledgeCategoryEntity, UUID> {
}
