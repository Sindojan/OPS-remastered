package com.sindoflow.ops.machines.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateMachineRequest(
        String name,
        String type,
        UUID stationId,
        BigDecimal capacityPerHour,
        String manufacturer,
        String model,
        String serialNumber,
        LocalDate purchaseDate
) {}
