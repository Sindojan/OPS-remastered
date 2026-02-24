package com.owlsburg.ops.production;

import com.owlsburg.ops.production.dto.*;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class QualityCheckService {

    private static final Logger log = LoggerFactory.getLogger(QualityCheckService.class);

    private final QualityCheckRepository qualityCheckRepository;
    private final QualityDefectRepository qualityDefectRepository;
    private final JobRepository jobRepository;

    public QualityCheckService(QualityCheckRepository qualityCheckRepository,
                               QualityDefectRepository qualityDefectRepository,
                               JobRepository jobRepository) {
        this.qualityCheckRepository = qualityCheckRepository;
        this.qualityDefectRepository = qualityDefectRepository;
        this.jobRepository = jobRepository;
    }

    @Transactional
    public QualityCheckResponse create(CreateQualityCheckRequest request) {
        if (!jobRepository.existsById(request.jobId())) {
            throw new EntityNotFoundException("Job not found: " + request.jobId());
        }

        QualityCheckEntity entity = new QualityCheckEntity();
        entity.setJobId(request.jobId());
        entity.setCheckedBy(request.checkedBy());
        entity.setCheckType(request.checkType());
        entity.setResult(request.result());
        entity.setDefectCount(request.defectCount());
        entity.setNotes(request.notes());
        entity.setCheckedAt(Instant.now());
        entity = qualityCheckRepository.save(entity);
        log.info("Created quality check: {} for job {}", entity.getId(), request.jobId());
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<QualityCheckResponse> findByJob(UUID jobId) {
        return qualityCheckRepository.findByJobId(jobId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public QualityCheckResponse addDefect(UUID qualityCheckId, CreateQualityDefectRequest request) {
        QualityCheckEntity check = qualityCheckRepository.findById(qualityCheckId)
                .orElseThrow(() -> new EntityNotFoundException("Quality check not found: " + qualityCheckId));

        QualityDefectEntity defect = new QualityDefectEntity();
        defect.setQualityCheckId(qualityCheckId);
        defect.setDefectType(request.defectType());
        defect.setDescription(request.description());
        defect.setSeverity(request.severity());
        qualityDefectRepository.save(defect);

        log.info("Added defect to quality check: {}", qualityCheckId);
        return toResponse(qualityCheckRepository.findById(qualityCheckId).orElseThrow());
    }

    private QualityCheckResponse toResponse(QualityCheckEntity entity) {
        List<QualityDefectResponse> defects = entity.getDefects() != null
                ? entity.getDefects().stream()
                    .map(d -> new QualityDefectResponse(
                            d.getId(), d.getDefectType(), d.getDescription(), d.getSeverity()))
                    .toList()
                : List.of();

        int computedDefectCount = defects.size();

        return new QualityCheckResponse(
                entity.getId(),
                entity.getJobId(),
                entity.getCheckedBy(),
                entity.getCheckType(),
                entity.getResult(),
                computedDefectCount,
                entity.getNotes(),
                entity.getCheckedAt(),
                defects
        );
    }
}
