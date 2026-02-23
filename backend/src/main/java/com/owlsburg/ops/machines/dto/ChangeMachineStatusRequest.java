package com.owlsburg.ops.machines.dto;

import com.owlsburg.ops.machines.MachineStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeMachineStatusRequest(
        @NotNull MachineStatus newStatus
) {}
