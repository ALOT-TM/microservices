package com.fluxusbackend.companyretail.infrastructure.clients.dto;

import java.time.Instant;

public record DonationDto(
        Object donationId,
        Integer quantity,
        String status,
        Instant createdAt
) {
}
