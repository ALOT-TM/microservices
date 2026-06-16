package com.fluxusbackend.donationlogistics.infrastructure.clients;

import com.fluxusbackend.donationlogistics.infrastructure.clients.dto.BeneficiaryInstitutionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "beneficiaries-service", url = "${services.beneficiaries.base-url:http://localhost:8102}")
public interface BeneficiaryInstitutionClient {
    @GetMapping("/api/beneficiary-institutions/{beneficiaryId}")
    BeneficiaryInstitutionDto getBeneficiaryInstitution(@PathVariable("beneficiaryId") Long beneficiaryId);
}
