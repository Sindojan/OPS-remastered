package com.owlsburg.ops.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KnowledgeTagRepository extends JpaRepository<KnowledgeTagEntity, UUID> {

    Optional<KnowledgeTagEntity> findByName(String name);

    List<KnowledgeTagEntity> findByIdIn(Collection<UUID> ids);
}
