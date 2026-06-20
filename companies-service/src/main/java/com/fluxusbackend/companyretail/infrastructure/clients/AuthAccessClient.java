package com.fluxusbackend.companyretail.infrastructure.clients;

import com.fluxusbackend.companyretail.infrastructure.clients.dto.RetailUserDto;
import com.fluxusbackend.companyretail.infrastructure.clients.dto.RoleDto;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "authaccess-service")
public interface AuthAccessClient {

    @GetMapping("/api/auth/retail-users")
    List<RetailUserDto> listRetailUsers();

    @GetMapping("/api/auth/roles")
    List<RoleDto> listRoles();
}
