package com.owlsburg.ops.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        UserResponse user
) {}
