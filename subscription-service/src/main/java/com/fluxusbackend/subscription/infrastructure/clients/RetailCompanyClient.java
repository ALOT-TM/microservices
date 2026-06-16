package com.fluxusbackend.subscription.infrastructure.clients;

import com.fluxusbackend.subscription.infrastructure.clients.dto.RetailCompanyDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "companies-service", url = "${services.companies.base-url:http://localhost:8101}")
public interface RetailCompanyClient {
    @GetMapping("/api/retail-companies/{companyId}")
    RetailCompanyDto getRetailCompany(@PathVariable("companyId") Long companyId);
}
