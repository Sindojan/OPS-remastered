package com.sindoflow.ops.bom;

import com.sindoflow.ops.bom.dto.CreateProcessPlanRequest;
import com.sindoflow.ops.bom.dto.ProcessStepRequest;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProcessPlanService {

    private static final Logger log = LoggerFactory.getLogger(ProcessPlanService.class);

    private final ProcessPlanRepository planRepository;
    private final ProcessStepRepository stepRepository;
    private final PartRepository partRepository;

    public ProcessPlanService(ProcessPlanRepository planRepository,
                              ProcessStepRepository stepRepository,
                              PartRepository partRepository) {
        this.planRepository = planRepository;
        this.stepRepository = stepRepository;
        this.partRepository = partRepository;
    }

    @Transactional
    public ProcessPlanEntity createPlan(CreateProcessPlanRequest request) {
        if (!partRepository.existsById(request.partId())) {
            throw new EntityNotFoundException("Part not found: " + request.partId());
        }
        ProcessPlanEntity entity = new ProcessPlanEntity();
        entity.setPartId(request.partId());
        entity.setVersionNumber(request.versionNumber());
        entity.setName(request.name());
        entity.setValidFrom(request.validFrom());
        entity.setCreatedBy(request.createdBy());
        return planRepository.save(entity);
    }

    @Transactional
    public ProcessStepEntity addStep(UUID planId, ProcessStepRequest request) {
        if (!planRepository.existsById(planId)) {
            throw new EntityNotFoundException("Process plan not found: " + planId);
        }
        ProcessStepEntity entity = new ProcessStepEntity();
        entity.setProcessPlanId(planId);
        entity.setStepNumber(request.stepNumber());
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setStationId(request.stationId());
        entity.setMachineId(request.machineId());
        entity.setSetupTimeMinutes(request.setupTimeMinutes());
        entity.setProcessingTimeMinutes(request.processingTimeMinutes());
        entity.setNotes(request.notes());
        return stepRepository.save(entity);
    }

    @Transactional
    public ProcessStepEntity updateStep(UUID stepId, ProcessStepRequest request) {
        ProcessStepEntity entity = stepRepository.findById(stepId)
                .orElseThrow(() -> new EntityNotFoundException("Process step not found: " + stepId));
        entity.setStepNumber(request.stepNumber());
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setStationId(request.stationId());
        entity.setMachineId(request.machineId());
        entity.setSetupTimeMinutes(request.setupTimeMinutes());
        entity.setProcessingTimeMinutes(request.processingTimeMinutes());
        entity.setNotes(request.notes());
        return stepRepository.save(entity);
    }

    @Transactional
    public void removeStep(UUID stepId) {
        if (!stepRepository.existsById(stepId)) {
            throw new EntityNotFoundException("Process step not found: " + stepId);
        }
        stepRepository.deleteById(stepId);
    }

    @Transactional
    public ProcessPlanEntity activatePlan(UUID planId) {
        ProcessPlanEntity plan = planRepository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("Process plan not found: " + planId));

        // Archive all other active plans for this part
        List<ProcessPlanEntity> others = planRepository.findByPartId(plan.getPartId());
        for (ProcessPlanEntity other : others) {
            if (!other.getId().equals(planId) && other.getStatus() == VersionStatus.ACTIVE) {
                other.setStatus(VersionStatus.ARCHIVED);
                planRepository.save(other);
            }
        }

        plan.setStatus(VersionStatus.ACTIVE);
        return planRepository.save(plan);
    }

    @Transactional(readOnly = true)
    public ProcessPlanEntity getPlanById(UUID id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Process plan not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<ProcessStepEntity> getSteps(UUID planId) {
        return stepRepository.findByProcessPlanIdOrderByStepNumberAsc(planId);
    }
}
