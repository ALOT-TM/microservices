package com.fluxusbackend.authaccess.infrastructure.bootstrap;

import com.fluxusbackend.authaccess.domain.model.aggregates.Permission;
import com.fluxusbackend.authaccess.infrastructure.persistence.jpa.repositories.PermissionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(1)
public class AuthAccessCatalogSeeder implements CommandLineRunner {

    private final PermissionRepository permissionRepository;

    public AuthAccessCatalogSeeder(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        var permissions = List.of(
                new Permission((short) 1, "Todo el sistema"),
                new Permission((short) 2, "Dashboard"),
                new Permission((short) 3, "Merma"),
                new Permission((short) 4, "Donaciones"),
                new Permission((short) 5, "Locales"),
                new Permission((short) 6, "Usuarios y Roles")
        );

        for (var perm : permissions) {
            if (permissionRepository.findByDescription(perm.getDescription()).isEmpty()) {
                permissionRepository.save(perm);
            }
        }
    }
}
