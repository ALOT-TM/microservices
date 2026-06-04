package com.fluxusbackend.authaccess.application.internal.commandservices;

import com.fluxusbackend.authaccess.application.internal.services.RemoteReferenceValidator;
import com.fluxusbackend.authaccess.domain.model.aggregates.BeneficiaryUser;
import com.fluxusbackend.authaccess.domain.model.aggregates.Permission;
import com.fluxusbackend.authaccess.domain.model.aggregates.RetailUser;
import com.fluxusbackend.authaccess.domain.model.aggregates.Role;
import com.fluxusbackend.authaccess.domain.model.aggregates.RolePermission;
import com.fluxusbackend.authaccess.domain.model.aggregates.UserAccount;
import com.fluxusbackend.authaccess.domain.model.commands.ChangePasswordCommand;
import com.fluxusbackend.authaccess.domain.model.commands.RegisterUserCommand;
import com.fluxusbackend.authaccess.domain.model.commands.UpdateProfileCommand;
import com.fluxusbackend.authaccess.domain.model.enums.UserActor;
import com.fluxusbackend.authaccess.domain.model.valueobjects.PasswordHash;
import com.fluxusbackend.authaccess.domain.services.UserCommandService;
import com.fluxusbackend.authaccess.infrastructure.persistence.jpa.repositories.PermissionRepository;
import com.fluxusbackend.authaccess.infrastructure.persistence.jpa.repositories.RolePermissionRepository;
import com.fluxusbackend.authaccess.infrastructure.persistence.jpa.repositories.RoleRepository;
import com.fluxusbackend.authaccess.infrastructure.persistence.jpa.repositories.UserAccountRepository;
import jakarta.transaction.Transactional;
import java.util.NoSuchElementException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserCommandServiceImpl implements UserCommandService {

    private final UserAccountRepository repository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RemoteReferenceValidator referenceValidator;

    public UserCommandServiceImpl(
            UserAccountRepository repository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            RolePermissionRepository rolePermissionRepository,
            BCryptPasswordEncoder passwordEncoder,
            RemoteReferenceValidator referenceValidator
    ) {
        this.repository = repository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.passwordEncoder = passwordEncoder;
        this.referenceValidator = referenceValidator;
    }

    @Override
    @Transactional
    public UserAccount handle(RegisterUserCommand command) {
        var existing = repository.findByEmailValue(command.email().value());
        if (existing.isPresent()) {
            throw new NoSuchElementException("Email already registered");
        }
        var hash = new PasswordHash(passwordEncoder.encode(command.rawPassword()));
        var user = new UserAccount(command.email(), hash, command.username());

        if (command.actor() == UserActor.RETAIL) {
            referenceValidator.requireRetailCompany(command.retailCompanyId());
            var role = resolveDefaultRetailRole(command.retailCompanyId());
            var retailUser = new RetailUser(user, command.retailCompanyId(), role, true);
            user.attachRetailUser(retailUser);
        } else {
            referenceValidator.requireBeneficiaryInstitution(command.beneficiaryInstitutionId());
            var beneficiaryUser = new BeneficiaryUser(user, command.beneficiaryInstitutionId());
            user.attachBeneficiaryUser(beneficiaryUser);
        }

        return repository.save(user);
    }

    private Role resolveDefaultRetailRole(Long retailCompanyId) {
        var role = roleRepository.findFirstByRetailCompanyId(retailCompanyId)
                .orElseGet(() -> roleRepository.save(new Role(retailCompanyId, "RETAIL_FULL_ACCESS")));
        ensureRoleHasAllPermissions(role);
        return role;
    }

    private void ensureRoleHasAllPermissions(Role role) {
        var permissions = permissionRepository.findAll();
        if (permissions.isEmpty()) {
            var defaultPermission = new Permission((short) 1, "FULL_ACCESS");
            permissions = java.util.List.of(permissionRepository.save(defaultPermission));
        }
        for (Permission permission : permissions) {
            if (!rolePermissionRepository.existsByRoleAndPermission(role, permission)) {
                rolePermissionRepository.save(new RolePermission(role, permission));
            }
        }
    }

    @Override
    @Transactional
    public UserAccount handle(UpdateProfileCommand command) {
        var user = repository.findById(command.userId())
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        var existing = repository.findByEmailValue(command.email());
        if (existing.isPresent() && !existing.get().getUserId().value().equals(command.userId())) {
            throw new IllegalArgumentException("Email already in use");
        }
        user.updateProfile(command.username(), new com.fluxusbackend.authaccess.domain.model.valueobjects.EmailAddress(command.email()));
        return repository.save(user);
    }

    @Override
    @Transactional
    public void handle(ChangePasswordCommand command) {
        var user = repository.findById(command.userId())
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        if (!passwordEncoder.matches(command.currentPassword(), user.getPasswordHash().value())) {
            throw new IllegalArgumentException("Current password does not match");
        }
        var newHash = new PasswordHash(passwordEncoder.encode(command.newPassword()));
        user.updatePassword(newHash);
        repository.save(user);
    }
}
