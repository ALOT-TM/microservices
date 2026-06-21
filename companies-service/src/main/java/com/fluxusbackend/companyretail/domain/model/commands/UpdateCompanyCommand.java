package com.fluxusbackend.companyretail.domain.model.commands;

import java.util.Objects;

public record UpdateCompanyCommand(Long id, String name) {
    public UpdateCompanyCommand {
        Objects.requireNonNull(id, "Company ID is required");
        Objects.requireNonNull(name, "Company name is required");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Company name is required");
        }
    }
}
