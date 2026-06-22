package com.fluxusbackend.companyretail.domain.services;

import com.fluxusbackend.companyretail.domain.model.aggregates.RetailCompany;
import com.fluxusbackend.companyretail.domain.model.commands.CreateCompanyCommand;
import com.fluxusbackend.companyretail.domain.model.commands.UpdateCompanyCommand;

public interface CompanyCommandService {
    RetailCompany handle(CreateCompanyCommand command);
    RetailCompany handle(UpdateCompanyCommand command);
}

