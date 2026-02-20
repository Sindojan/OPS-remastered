package com.sindoflow.ops.bom;

import com.sindoflow.ops.bom.dto.BomItemRequest;
import com.sindoflow.ops.bom.dto.CreateBomVersionRequest;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BomService {

    private static final Logger log = LoggerFactory.getLogger(BomService.class);

    private final BomVersionRepository bomVersionRepository;
    private final BomItemRepository bomItemRepository;
    private final PartRepository partRepository;

    public BomService(BomVersionRepository bomVersionRepository,
                      BomItemRepository bomItemRepository,
                      PartRepository partRepository) {
        this.bomVersionRepository = bomVersionRepository;
        this.bomItemRepository = bomItemRepository;
        this.partRepository = partRepository;
    }

    @Transactional
    public BomVersionEntity createVersion(CreateBomVersionRequest request) {
        if (!partRepository.existsById(request.partId())) {
            throw new EntityNotFoundException("Part not found: " + request.partId());
        }
        BomVersionEntity entity = new BomVersionEntity();
        entity.setPartId(request.partId());
        entity.setVersionNumber(request.versionNumber());
        entity.setValidFrom(request.validFrom());
        entity.setCreatedBy(request.createdBy());
        return bomVersionRepository.save(entity);
    }

    @Transactional
    public BomItemEntity addItem(UUID bomVersionId, BomItemRequest request) {
        if (!bomVersionRepository.existsById(bomVersionId)) {
            throw new EntityNotFoundException("BOM version not found: " + bomVersionId);
        }
        BomItemEntity entity = new BomItemEntity();
        entity.setBomVersionId(bomVersionId);
        entity.setComponentPartId(request.componentPartId());
        entity.setQuantity(request.quantity());
        entity.setUnitId(request.unitId());
        entity.setPosition(request.position());
        entity.setNotes(request.notes());
        return bomItemRepository.save(entity);
    }

    @Transactional
    public void removeItem(UUID itemId) {
        if (!bomItemRepository.existsById(itemId)) {
            throw new EntityNotFoundException("BOM item not found: " + itemId);
        }
        bomItemRepository.deleteById(itemId);
    }

    @Transactional
    public BomVersionEntity activateVersion(UUID bomVersionId) {
        BomVersionEntity version = bomVersionRepository.findById(bomVersionId)
                .orElseThrow(() -> new EntityNotFoundException("BOM version not found: " + bomVersionId));

        // Archive all other active versions for this part
        List<BomVersionEntity> others = bomVersionRepository.findByPartId(version.getPartId());
        for (BomVersionEntity other : others) {
            if (!other.getId().equals(bomVersionId) && other.getStatus() == VersionStatus.ACTIVE) {
                other.setStatus(VersionStatus.ARCHIVED);
                bomVersionRepository.save(other);
            }
        }

        version.setStatus(VersionStatus.ACTIVE);
        return bomVersionRepository.save(version);
    }

    @Transactional(readOnly = true)
    public BomVersionEntity getActiveVersionForPart(UUID partId) {
        return bomVersionRepository.findByPartIdAndStatus(partId, VersionStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("No active BOM version for part: " + partId));
    }

    @Transactional(readOnly = true)
    public List<BomItemEntity> getItems(UUID bomVersionId) {
        return bomItemRepository.findByBomVersionIdOrderByPositionAsc(bomVersionId);
    }

    @Transactional(readOnly = true)
    public BomVersionEntity getVersionById(UUID id) {
        return bomVersionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("BOM version not found: " + id));
    }
}
