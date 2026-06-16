package com.fluxusbackend.donationlogistics.infrastructure.clients.dto;

public record ShrinkageDto(
        Long shrinkageId,
        Long companyId,
        String status,
        String name
) {
}
