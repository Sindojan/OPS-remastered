package com.sindoflow.ops.bom;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Service
public class JobCalculationService {

    private static final Logger log = LoggerFactory.getLogger(JobCalculationService.class);

    private final JobCalculationRepository jobCalculationRepository;
    private final CalculationRepository calculationRepository;

    public JobCalculationService(JobCalculationRepository jobCalculationRepository,
                                 CalculationRepository calculationRepository) {
        this.jobCalculationRepository = jobCalculationRepository;
        this.calculationRepository = calculationRepository;
    }

    @Transactional
    public JobCalculationEntity createFromCalculation(UUID jobId, UUID calculationId) {
        if (!calculationRepository.existsById(calculationId)) {
            throw new EntityNotFoundException("Calculation not found: " + calculationId);
        }
        JobCalculationEntity entity = new JobCalculationEntity();
        entity.setJobId(jobId);
        entity.setCalculationId(calculationId);
        return jobCalculationRepository.save(entity);
    }

    @Transactional
    public JobCalculationEntity finalize(UUID jobCalculationId, BigDecimal actualMaterialCost,
                                          BigDecimal actualLaborCost) {
        JobCalculationEntity entity = jobCalculationRepository.findById(jobCalculationId)
                .orElseThrow(() -> new EntityNotFoundException("Job calculation not found: " + jobCalculationId));

        if (entity.getFinalizedAt() != null) {
            throw new IllegalArgumentException("Job calculation already finalized");
        }

        CalculationEntity calculation = calculationRepository.findById(entity.getCalculationId())
                .orElseThrow(() -> new EntityNotFoundException("Calculation not found: " + entity.getCalculationId()));

        BigDecimal actualTotal = actualMaterialCost.add(actualLaborCost);
        BigDecimal plannedTotal = calculation.getTotalCost();

        BigDecimal variancePercent = BigDecimal.ZERO;
        if (plannedTotal.compareTo(BigDecimal.ZERO) != 0) {
            variancePercent = actualTotal.subtract(plannedTotal)
                    .divide(plannedTotal, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        entity.setActualMaterialCost(actualMaterialCost);
        entity.setActualLaborCost(actualLaborCost);
        entity.setActualTotalCost(actualTotal);
        entity.setVariancePercent(variancePercent);
        entity.setFinalizedAt(Instant.now());
        return jobCalculationRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public JobCalculationEntity getById(UUID id) {
        return jobCalculationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Job calculation not found: " + id));
    }
}
