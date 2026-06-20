package com.fluxusbackend.beneficiary.infrastructure.clients;

import com.fluxusbackend.beneficiary.infrastructure.clients.dto.AddressDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "companies-service")
public interface AddressClient {

    @GetMapping("/api/addresses/{addressId}")
    AddressDto getById(@PathVariable Long addressId);
}
