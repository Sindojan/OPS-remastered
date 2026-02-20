package com.sindoflow.ops.auth.dto;

import com.sindoflow.ops.auth.Role;
import jakarta.validation.constraints.NotNull;

public record RoleUpdateRequest(
        @NotNull Role role
) {}
