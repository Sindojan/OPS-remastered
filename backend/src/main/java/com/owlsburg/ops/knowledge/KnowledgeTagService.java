package com.owlsburg.ops.knowledge;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class KnowledgeTagService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeTagService.class);

    private final KnowledgeTagRepository tagRepository;

    public KnowledgeTagService(KnowledgeTagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional(readOnly = true)
    public List<KnowledgeTagEntity> findAll() {
        return tagRepository.findAll();
    }

    @Transactional
    public KnowledgeTagEntity findOrCreate(String name) {
        return tagRepository.findByName(name)
                .orElseGet(() -> {
                    KnowledgeTagEntity entity = new KnowledgeTagEntity();
                    entity.setName(name);
                    log.info("Creating knowledge tag: {}", name);
                    return tagRepository.save(entity);
                });
    }

    @Transactional(readOnly = true)
    public Set<KnowledgeTagEntity> findByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(tagRepository.findByIdIn(ids));
    }

    @Transactional
    public void delete(UUID id) {
        if (!tagRepository.existsById(id)) {
            throw new EntityNotFoundException("Knowledge tag not found: " + id);
        }
        tagRepository.deleteById(id);
        log.info("Deleted knowledge tag: {}", id);
    }
}
