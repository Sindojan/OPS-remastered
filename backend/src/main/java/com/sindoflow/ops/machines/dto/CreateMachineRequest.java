package com.sindoflow.ops.machines.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateMachineRequest(
        @NotBlank String name,
        @NotBlank String machineNumber,
        String type,
        UUID stationId,
        BigDecimal capacityPerHour,
        String manufacturer,
        String model,
        String serialNumber,
        LocalDate purchaseDate
) {}
