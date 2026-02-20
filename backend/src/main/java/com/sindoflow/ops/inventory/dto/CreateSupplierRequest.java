package com.sindoflow.ops.inventory.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSupplierRequest(
        @NotBlank String name,
        String contactName,
        String email,
        String phone,
        String address,
        String taxId
) {}
