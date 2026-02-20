package com.sindoflow.ops.bom.dto;

import com.sindoflow.ops.bom.ProcessStepEntity;

import java.util.UUID;

public record ProcessStepResponse(
        UUID id,
        UUID processPlanId,
        int stepNumber,
        String name,
        String description,
        UUID stationId,
        UUID machineId,
        int setupTimeMinutes,
        int processingTimeMinutes,
        String notes
) {
    public static ProcessStepResponse from(ProcessStepEntity e) {
        return new ProcessStepResponse(
                e.getId(), e.getProcessPlanId(), e.getStepNumber(),
                e.getName(), e.getDescription(), e.getStationId(), e.getMachineId(),
                e.getSetupTimeMinutes(), e.getProcessingTimeMinutes(), e.getNotes()
        );
    }
}
