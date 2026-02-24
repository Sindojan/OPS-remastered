package com.owlsburg.ops.people.dto;

import java.util.UUID;

public record EmployeeCreateResponse(
        EmployeeResponse employee,
        UserCredentials userCredentials
) {
    public record UserCredentials(
            UUID userId,
            String email,
            String role,
            String password
    ) {}
}
