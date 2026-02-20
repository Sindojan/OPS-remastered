package com.sindoflow.ops.people.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record QualificationRequest(
        @NotBlank String qualification,
        LocalDate certifiedAt,
        LocalDate expiresAt
) {}
