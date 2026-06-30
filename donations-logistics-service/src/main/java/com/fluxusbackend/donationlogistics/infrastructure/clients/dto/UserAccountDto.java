package com.fluxusbackend.donationlogistics.infrastructure.clients.dto;

public record UserAccountDto(
    Long id,
    String email,
    String username,
    String actor,
    Long retailCompanyId,
    Long beneficiaryInstitutionId
) {}
