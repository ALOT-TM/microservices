package com.fluxusbackend.donationlogistics.application.internal.outboundservices.acl;

import com.fluxusbackend.donationlogistics.domain.model.valueobjects.BeneficiaryReferenceId;
import com.fluxusbackend.donationlogistics.infrastructure.clients.BeneficiaryInstitutionClient;
import feign.FeignException;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ExternalBeneficiaryService {

    private final BeneficiaryInstitutionClient beneficiaryInstitutionClient;

    public ExternalBeneficiaryService(BeneficiaryInstitutionClient beneficiaryInstitutionClient) {
        this.beneficiaryInstitutionClient = beneficiaryInstitutionClient;
    }

    public Optional<BeneficiaryReferenceId> fetchBeneficiaryById(Long beneficiaryId) {
        try {
            var beneficiary = beneficiaryInstitutionClient.getBeneficiaryInstitution(beneficiaryId);
            return beneficiary == null ? Optional.empty() : Optional.of(new BeneficiaryReferenceId(beneficiary.beneficiaryInstitutionId()));
        } catch (FeignException.NotFound ex) {
            return Optional.empty();
        }
    }

    public String fetchBeneficiaryNameById(Long beneficiaryId) {
        try {
            var beneficiary = beneficiaryInstitutionClient.getBeneficiaryInstitution(beneficiaryId);
            return beneficiary == null ? "Unknown beneficiary" : beneficiary.name();
        } catch (FeignException ex) {
            return "Unknown beneficiary";
        }
    }
}


