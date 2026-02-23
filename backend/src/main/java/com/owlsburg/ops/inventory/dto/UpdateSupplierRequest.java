package com.owlsburg.ops.inventory.dto;

public record UpdateSupplierRequest(
        String name,
        String contactName,
        String email,
        String phone,
        String address,
        String taxId,
        String status
) {}
