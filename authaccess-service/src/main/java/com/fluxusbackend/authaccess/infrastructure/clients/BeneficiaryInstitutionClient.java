package com.fluxusbackend.authaccess.infrastructure.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "beneficiary-institution-service", url = "${services.beneficiary.base-url}")
public interface BeneficiaryInstitutionClient {
    @GetMapping("/api/beneficiary-institutions/{id}")
    void getBeneficiaryInstitution(@PathVariable("id") Long id);
}
