package com.fluxusbackend.donationlogistics.infrastructure.clients;

import com.fluxusbackend.donationlogistics.infrastructure.clients.dto.UserAccountDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@FeignClient(name = "authaccess-service", url = "${services.authaccess.base-url:http://localhost:8094}")
public interface AuthAccessClient {
    @GetMapping("/api/auth/users")
    List<UserAccountDto> listUsers();
}
