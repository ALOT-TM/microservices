package com.fluxusbackend.authaccess.interfaces.rest.transform;

import com.fluxusbackend.authaccess.application.internal.services.AuthorizationService;
import com.fluxusbackend.authaccess.application.internal.services.RemoteReferenceValidator;
import com.fluxusbackend.authaccess.domain.model.enums.UserActor;
import com.fluxusbackend.authaccess.domain.model.aggregates.Role;
import com.fluxusbackend.authaccess.domain.model.aggregates.Permission;
import com.fluxusbackend.authaccess.domain.model.aggregates.RolePermission;
import com.fluxusbackend.authaccess.infrastructure.persistence.jpa.repositories.PermissionRepository;
import com.fluxusbackend.authaccess.infrastructure.persistence.jpa.repositories.RoleRepository;
import com.fluxusbackend.authaccess.infrastructure.persistence.jpa.repositories.RolePermissionRepository;
import com.fluxusbackend.authaccess.infrastructure.persistence.jpa.repositories.RetailUserRepository;
import jakarta.transaction.Transactional;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/auth/roles")
@Tag(name = "Roles Management", description = "CRUD operations on roles")
@SecurityRequirement(name = "bearer")
public class RoleController {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final RetailUserRepository retailUserRepository;
    private final AuthorizationService authorizationService;
    private final RemoteReferenceValidator referenceValidator;

    public RoleController(
            RoleRepository roleRepository,
            RolePermissionRepository rolePermissionRepository,
            PermissionRepository permissionRepository,
            RetailUserRepository retailUserRepository,
            AuthorizationService authorizationService,
            RemoteReferenceValidator referenceValidator
    ) {
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionRepository = permissionRepository;
        this.retailUserRepository = retailUserRepository;
        this.authorizationService = authorizationService;
        this.referenceValidator = referenceValidator;
    }

    @GetMapping
    @Operation(summary = "List roles for current retail company")
    public List<RoleDto> listRoles() {
        authorizationService.requireActor(UserActor.RETAIL);
        Long companyId = authorizationService.getCurrentUserCompanyId().value();
        return roleRepository.findByRetailCompanyId(companyId).stream()
                .map(role -> {
                    var permissions = rolePermissionRepository.findByRole(role).stream()
                            .map(rp -> rp.getPermission().getDescription())
                            .toList();
                    long count = retailUserRepository.countByRole(role);
                    return new RoleDto(role.getRoleId(), role.getName(), permissions, count);
                })
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create role for current company")
    @Transactional
    public RoleDto createRole(@RequestBody CreateRolePayload payload) {
        authorizationService.requireActor(UserActor.RETAIL);
        Long companyId = authorizationService.getCurrentUserCompanyId().value();
        referenceValidator.requireRetailCompany(companyId);
        var role = new Role(companyId, payload.name());
        var saved = roleRepository.save(role);

        if (payload.permissions() != null) {
            for (String permName : payload.permissions()) {
                permissionRepository.findByDescription(permName).ifPresent(permission -> {
                    rolePermissionRepository.save(new RolePermission(saved, permission));
                });
            }
        }

        var permissions = rolePermissionRepository.findByRole(saved).stream()
                .map(rp -> rp.getPermission().getDescription())
                .toList();
        return new RoleDto(saved.getRoleId(), saved.getName(), permissions, 0L);
    }

    @PutMapping("/{roleId}")
    @Operation(summary = "Update role details")
    @Transactional
    public RoleDto updateRole(@PathVariable Long roleId, @RequestBody CreateRolePayload payload) {
        authorizationService.requireActor(UserActor.RETAIL);
        Long companyId = authorizationService.getCurrentUserCompanyId().value();
        var role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        if (!role.getRetailCompanyId().equals(companyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden access");
        }
        role.rename(payload.name());
        var saved = roleRepository.save(role);

        rolePermissionRepository.deleteByRole(saved);
        if (payload.permissions() != null) {
            for (String permName : payload.permissions()) {
                permissionRepository.findByDescription(permName).ifPresent(permission -> {
                    rolePermissionRepository.save(new RolePermission(saved, permission));
                });
            }
        }

        var permissions = rolePermissionRepository.findByRole(saved).stream()
                .map(rp -> rp.getPermission().getDescription())
                .toList();
        long count = retailUserRepository.countByRole(saved);
        return new RoleDto(saved.getRoleId(), saved.getName(), permissions, count);
    }

    @DeleteMapping("/{roleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete role")
    @Transactional
    public void deleteRole(@PathVariable Long roleId) {
        authorizationService.requireActor(UserActor.RETAIL);
        Long companyId = authorizationService.getCurrentUserCompanyId().value();
        var role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        if (!role.getRetailCompanyId().equals(companyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden access");
        }
        
        long count = retailUserRepository.countByRole(role);
        if (count > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede eliminar el rol porque está asignado a uno o más usuarios.");
        }
        
        rolePermissionRepository.deleteByRole(role);
        roleRepository.delete(role);
    }

    public record RoleDto(Long roleId, String name, List<String> permissions, Long userCount) {
    }

    public record CreateRolePayload(String name, List<String> permissions) {
    }
}
