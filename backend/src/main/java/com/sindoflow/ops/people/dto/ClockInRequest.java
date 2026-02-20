package com.sindoflow.ops.people.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ClockInRequest(
        @NotNull UUID employeeId
) {}
