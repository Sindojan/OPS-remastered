package com.sindoflow.ops.bom;

import com.sindoflow.ops.bom.dto.CreatePartRequest;
import com.sindoflow.ops.bom.dto.UpdatePartRequest;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PartService {

    private static final Logger log = LoggerFactory.getLogger(PartService.class);

    private final PartRepository partRepository;

    public PartService(PartRepository partRepository) {
        this.partRepository = partRepository;
    }

    @Transactional
    public PartEntity create(CreatePartRequest request) {
        if (partRepository.existsByPartNumber(request.partNumber())) {
            throw new IllegalArgumentException("Part number already exists: " + request.partNumber());
        }
        PartEntity entity = new PartEntity();
        entity.setPartNumber(request.partNumber());
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setType(PartType.valueOf(request.type()));
        entity.setUnitId(request.unitId());
        if (request.status() != null) entity.setStatus(request.status());
        return partRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public PartEntity getById(UUID id) {
        return partRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Part not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<PartEntity> findAll(Pageable pageable) {
        return partRepository.findAll(pageable);
    }

    @Transactional
    public PartEntity update(UUID id, UpdatePartRequest request) {
        PartEntity entity = getById(id);
        if (request.name() != null) entity.setName(request.name());
        if (request.description() != null) entity.setDescription(request.description());
        if (request.type() != null) entity.setType(PartType.valueOf(request.type()));
        if (request.unitId() != null) entity.setUnitId(request.unitId());
        if (request.status() != null) entity.setStatus(request.status());
        return partRepository.save(entity);
    }

    @Transactional
    public void delete(UUID id) {
        if (!partRepository.existsById(id)) {
            throw new EntityNotFoundException("Part not found: " + id);
        }
        partRepository.deleteById(id);
    }
}
