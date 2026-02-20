package com.sindoflow.ops.machines.dto;

import com.sindoflow.ops.machines.MachineStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MachineResponse(
        UUID id,
        String name,
        String machineNumber,
        String type,
        UUID stationId,
        MachineStatus status,
        BigDecimal capacityPerHour,
        String manufacturer,
        String model,
        String serialNumber,
        LocalDate purchaseDate,
        Instant createdAt,
        Instant updatedAt
) {}
