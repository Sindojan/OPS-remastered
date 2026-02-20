package com.sindoflow.ops.auth.dto;

import jakarta.validation.constraints.Email;

public record UserUpdateRequest(
        @Email String email,
        String firstName,
        String lastName
) {}
