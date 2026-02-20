package com.sindoflow.ops.machines.dto;

import com.sindoflow.ops.machines.MachineStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeMachineStatusRequest(
        @NotNull MachineStatus newStatus
) {}
