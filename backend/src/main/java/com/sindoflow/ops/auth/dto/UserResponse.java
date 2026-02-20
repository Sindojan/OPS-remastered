package com.sindoflow.ops.auth.dto;

import com.sindoflow.ops.auth.Role;
import com.sindoflow.ops.auth.UserEntity;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        Role role,
        boolean active,
        Instant createdAt
) {
    public static UserResponse from(UserEntity entity) {
        return new UserResponse(
                entity.getId(),
                entity.getEmail(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getRole(),
                entity.isActive(),
                entity.getCreatedAt()
        );
    }
}
