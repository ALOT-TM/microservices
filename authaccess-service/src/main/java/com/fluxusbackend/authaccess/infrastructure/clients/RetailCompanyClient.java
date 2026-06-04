package com.fluxusbackend.authaccess.infrastructure.clients;

import com.fluxusbackend.authaccess.infrastructure.clients.dto.RetailCompanyDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "retail-company-service", url = "${services.retail-company.base-url}")
public interface RetailCompanyClient {
    @GetMapping("/api/retail-companies/{id}")
    RetailCompanyDto getRetailCompany(@PathVariable("id") Long id);
}
