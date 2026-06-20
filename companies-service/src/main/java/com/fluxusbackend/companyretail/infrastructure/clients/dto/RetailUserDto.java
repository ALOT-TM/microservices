package com.fluxusbackend.companyretail.infrastructure.clients.dto;

public record RetailUserDto(
        Long id,
        String email,
        String username,
        String actor,
        Long retailCompanyId,
        Long roleId,
        Long beneficiaryInstitutionId,
        Boolean retailUserActive
) {
}
