package com.fluxusbackend.shrinkage.infrastructure.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "companies-service", url = "${services.companies.base-url:http://localhost:8101}")
public interface RetailCompanyHeadquarterClient {
    @GetMapping("/api/retail-company-headquarters/{headquarterId}/company-id")
    Long getCompanyIdByHeadquarterId(@PathVariable("headquarterId") Long headquarterId);
}
