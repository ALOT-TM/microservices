package com.fluxusbackend.companyretail.infrastructure.clients;

import com.fluxusbackend.companyretail.infrastructure.clients.dto.DonationDto;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "donations-logistics-service")
public interface DonationClient {

    @GetMapping("/api/donations/company")
    List<DonationDto> listByCompany();
}
