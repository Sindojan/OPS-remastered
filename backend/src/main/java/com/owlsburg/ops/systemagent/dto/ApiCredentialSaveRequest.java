package com.owlsburg.ops.systemagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApiCredentialSaveRequest(
        @NotBlank @Size(max = 500) String value,
        @Size(max = 500) String description,
        String metadata
) {}
