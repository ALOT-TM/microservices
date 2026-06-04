package com.fluxusbackend.authaccess.interfaces.rest.transform;

import com.fluxusbackend.authaccess.application.internal.services.AuthorizationService;
import com.fluxusbackend.authaccess.application.internal.services.RemoteReferenceValidator;
import com.fluxusbackend.authaccess.domain.model.aggregates.RetailUser;
import com.fluxusbackend.authaccess.domain.model.dto.UserAccountDto;
import com.fluxusbackend.authaccess.domain.model.enums.UserActor;
import com.fluxusbackend.authaccess.domain.model.valueobjects.EmailAddress;
import com.fluxusbackend.authaccess.domain.model.valueobjects.PasswordHash;
import com.fluxusbackend.authaccess.infrastructure.persistence.jpa.repositories.RetailUserRepository;
import com.fluxusbackend.authaccess.infrastructure.persistence.jpa.repositories.RoleRepository;
import com.fluxusbackend.authaccess.infrastructure.persistence.jpa.repositories.UserAccountRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth/retail-users")
@Tag(name = "Retail Users Management", description = "CRUD operations on retail users")
@SecurityRequirement(name = "bearer")
public class RetailUserController {

    private final UserAccountRepository userAccountRepository;
    private final RetailUserRepository retailUserRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthorizationService authorizationService;
    private final RemoteReferenceValidator referenceValidator;

    public RetailUserController(
            UserAccountRepository userAccountRepository,
            RetailUserRepository retailUserRepository,
            RoleRepository roleRepository,
            BCryptPasswordEncoder passwordEncoder,
            AuthorizationService authorizationService,
            RemoteReferenceValidator referenceValidator
    ) {
        this.userAccountRepository = userAccountRepository;
        this.retailUserRepository = retailUserRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authorizationService = authorizationService;
        this.referenceValidator = referenceValidator;
    }

    @GetMapping
    @Operation(summary = "List retail users for current retail company")
    public List<UserAccountDto> listRetailUsers() {
        authorizationService.requireActor(UserActor.RETAIL);
        Long companyId = authorizationService.getCurrentUserCompanyId().value();
        return retailUserRepository.findByRetailCompanyId(companyId).stream()
                .map(ru -> UserAccountDto.from(ru.getUserAccount()))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create retail user")
    @Transactional
    public UserAccountDto createRetailUser(@Valid @RequestBody CreateRetailUserPayload payload) {
        authorizationService.requireActor(UserActor.RETAIL);
        Long companyId = authorizationService.getCurrentUserCompanyId().value();

        referenceValidator.requireRetailCompany(companyId);

        var existing = userAccountRepository.findByEmailValue(payload.email());
        if (existing.isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already registered");
        }

        var role = roleRepository.findById(payload.roleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        if (!role.getRetailCompanyId().equals(companyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden access to this role");
        }

        var hash = new PasswordHash(passwordEncoder.encode(payload.password()));
        var user = new com.fluxusbackend.authaccess.domain.model.aggregates.UserAccount(
                new EmailAddress(payload.email()),
                hash,
                payload.username()
        );
        var retailUser = new RetailUser(user, companyId, role, true);
        user.attachRetailUser(retailUser);

        var saved = userAccountRepository.save(user);
        return UserAccountDto.from(saved);
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update retail user details")
    @Transactional
    public UserAccountDto updateRetailUser(@PathVariable Long userId, @Valid @RequestBody UpdateRetailUserPayload payload) {
        authorizationService.requireActor(UserActor.RETAIL);
        Long companyId = authorizationService.getCurrentUserCompanyId().value();

        var user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        var retailUser = user.getRetailUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a retail user"));

        if (!retailUser.getRetailCompanyId().equals(companyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden access");
        }

        var existing = userAccountRepository.findByEmailValue(payload.email());
        if (existing.isPresent() && !existing.get().getUserId().value().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already in use");
        }

        var role = roleRepository.findById(payload.roleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        if (!role.getRetailCompanyId().equals(companyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden access to this role");
        }

        user.updateProfile(payload.username(), new EmailAddress(payload.email()));
        retailUser.updateRole(role);

        if (payload.password() != null && !payload.password().isBlank()) {
            var newHash = new PasswordHash(passwordEncoder.encode(payload.password()));
            user.updatePassword(newHash);
        }

        var saved = userAccountRepository.save(user);
        return UserAccountDto.from(saved);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete retail user")
    @Transactional
    public void deleteRetailUser(@PathVariable Long userId) {
        authorizationService.requireActor(UserActor.RETAIL);
        Long companyId = authorizationService.getCurrentUserCompanyId().value();

        var user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        var retailUser = user.getRetailUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a retail user"));

        if (!retailUser.getRetailCompanyId().equals(companyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden access");
        }

        userAccountRepository.delete(user);
    }

    @PatchMapping("/{userId}/status")
    @Operation(summary = "Toggle retail user status (active/inactive)")
    @Transactional
    public UserAccountDto toggleRetailUserStatus(@PathVariable Long userId) {
        authorizationService.requireActor(UserActor.RETAIL);
        Long companyId = authorizationService.getCurrentUserCompanyId().value();

        var user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        var retailUser = user.getRetailUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a retail user"));

        if (!retailUser.getRetailCompanyId().equals(companyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden access");
        }

        if (retailUser.isActive()) {
            retailUser.deactivate();
        } else {
            retailUser.activate();
        }

        var saved = userAccountRepository.save(user);
        return UserAccountDto.from(saved);
    }

    public record CreateRetailUserPayload(String username, String email, String password, Long roleId) {
    }

    public record UpdateRetailUserPayload(String username, String email, Long roleId, String password) {
    }
}
