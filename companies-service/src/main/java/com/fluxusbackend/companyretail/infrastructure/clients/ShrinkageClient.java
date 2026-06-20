package com.fluxusbackend.companyretail.infrastructure.clients;

import com.fluxusbackend.companyretail.infrastructure.clients.dto.ShrinkageDto;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "shrinkage-service")
public interface ShrinkageClient {

    @GetMapping("/api/shrinkages/company")
    List<ShrinkageDto> listByCompany();
}
