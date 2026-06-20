package com.fluxusbackend.companyretail.infrastructure.clients.dto;

import java.time.Instant;
import java.time.LocalDate;

public record ShrinkageDto(
        Long shrinkageId,
        CategoryDto category,
        ShrinkageReasonDto shrinkageReason,
        String name,
        Integer quantity,
        LocalDate expirationDate,
        String specificReason,
        String status,
        LocalDate pickupDate,
        Double shrinkageValue,
        Instant createdAt
) {
    public record CategoryDto(Long categoryId, String name) {
    }

    public record ShrinkageReasonDto(Long shrinkageReasonId, String name) {
    }
}
