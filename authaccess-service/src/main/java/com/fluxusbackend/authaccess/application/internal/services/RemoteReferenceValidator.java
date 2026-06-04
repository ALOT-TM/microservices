package com.fluxusbackend.authaccess.application.internal.services;

import com.fluxusbackend.authaccess.infrastructure.clients.BeneficiaryInstitutionClient;
import com.fluxusbackend.authaccess.infrastructure.clients.RetailCompanyClient;
import feign.FeignException;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

@Service
public class RemoteReferenceValidator {

    private final RetailCompanyClient retailCompanyClient;
    private final BeneficiaryInstitutionClient beneficiaryInstitutionClient;

    public RemoteReferenceValidator(
            RetailCompanyClient retailCompanyClient,
            BeneficiaryInstitutionClient beneficiaryInstitutionClient
    ) {
        this.retailCompanyClient = retailCompanyClient;
        this.beneficiaryInstitutionClient = beneficiaryInstitutionClient;
    }

    public void requireRetailCompany(Long retailCompanyId) {
        try {
            retailCompanyClient.getRetailCompany(retailCompanyId);
        } catch (FeignException.NotFound ex) {
            throw new NoSuchElementException("Retail company not found");
        } catch (FeignException ex) {
            throw new IllegalStateException("Retail company service unavailable");
        }
    }

    public void requireBeneficiaryInstitution(Long beneficiaryInstitutionId) {
        try {
            beneficiaryInstitutionClient.getBeneficiaryInstitution(beneficiaryInstitutionId);
        } catch (FeignException.NotFound ex) {
            throw new NoSuchElementException("Beneficiary institution not found");
        } catch (FeignException ex) {
            throw new IllegalStateException("Beneficiary institution service unavailable");
        }
    }
}
