package com.fluxusbackend.donationlogistics.infrastructure.clients;

import com.fluxusbackend.donationlogistics.infrastructure.clients.dto.ShrinkageDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "shrinkage-service", url = "${services.shrinkage.base-url:http://localhost:8103}")
public interface ShrinkageClient {
    @GetMapping("/api/shrinkages/{shrinkageId}")
    ShrinkageDto getShrinkage(@PathVariable("shrinkageId") Long shrinkageId);

    @PatchMapping("/api/shrinkages/{shrinkageId}/requested")
    ShrinkageDto markRequested(@PathVariable("shrinkageId") Long shrinkageId);

    @PatchMapping("/api/shrinkages/{shrinkageId}/donated")
    ShrinkageDto markDonated(@PathVariable("shrinkageId") Long shrinkageId);
}
