package com.fluxusbackend.authaccess.application.internal.services;

import com.fluxusbackend.authaccess.infrastructure.clients.BeneficiaryInstitutionClient;
import com.fluxusbackend.authaccess.infrastructure.clients.RetailCompanyClient;
import feign.FeignException;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RemoteReferenceValidator {

    private static final Logger log = LoggerFactory.getLogger(RemoteReferenceValidator.class);

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
        log.info("[VALIDATOR] Validating retail company with id={}", retailCompanyId);
        try {
            var company = retailCompanyClient.getRetailCompany(retailCompanyId);
            log.info("[VALIDATOR] Retail company found: id={}, name={}", company.id(), company.name());
        } catch (FeignException.NotFound ex) {
            log.warn("[VALIDATOR] Retail company NOT FOUND for id={}", retailCompanyId);
            throw new NoSuchElementException("Retail company not found");
        } catch (FeignException ex) {
            log.error("[VALIDATOR] Feign error calling retail company service: status={}, message={}", ex.status(), ex.getMessage());
            throw new IllegalStateException("Retail company service unavailable");
        }
    }

    public void requireBeneficiaryInstitution(Long beneficiaryInstitutionId) {
        log.info("[VALIDATOR] Validating beneficiary institution with id={}", beneficiaryInstitutionId);
        try {
            var institution = beneficiaryInstitutionClient.getBeneficiaryInstitution(beneficiaryInstitutionId);
            log.info("[VALIDATOR] Beneficiary institution found: id={}, name={}", institution.id(), institution.name());
        } catch (FeignException.NotFound ex) {
            log.warn("[VALIDATOR] Beneficiary institution NOT FOUND for id={}", beneficiaryInstitutionId);
            throw new NoSuchElementException("Beneficiary institution not found");
        } catch (FeignException ex) {
            log.error("[VALIDATOR] Feign error calling beneficiary service: status={}, message={}", ex.status(), ex.getMessage());
            throw new IllegalStateException("Beneficiary institution service unavailable");
        }
    }
}
