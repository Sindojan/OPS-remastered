package com.owlsburg.ops.auth.dto;

import com.owlsburg.ops.auth.Role;
import jakarta.validation.constraints.NotNull;

public record RoleUpdateRequest(
        @NotNull Role role
) {}
