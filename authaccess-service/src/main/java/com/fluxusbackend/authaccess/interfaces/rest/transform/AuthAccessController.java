package com.fluxusbackend.authaccess.interfaces.rest.transform;

import com.fluxusbackend.authaccess.domain.model.aggregates.UserAccount;
import com.fluxusbackend.authaccess.domain.model.commands.RegisterUserCommand;
import com.fluxusbackend.authaccess.domain.model.dto.AuthenticatedUser;
import com.fluxusbackend.authaccess.domain.model.dto.Profile;
import com.fluxusbackend.authaccess.domain.model.dto.UserAccountDto;
import com.fluxusbackend.authaccess.domain.model.queries.GetUserByIdQuery;
import com.fluxusbackend.authaccess.domain.model.queries.LoginUserQuery;
import com.fluxusbackend.authaccess.domain.model.valueobjects.UserId;
import com.fluxusbackend.authaccess.domain.services.UserAuthenticationQueryService;
import com.fluxusbackend.authaccess.domain.services.UserCommandService;
import com.fluxusbackend.authaccess.domain.services.UserQueryService;
import com.fluxusbackend.shared.application.security.AuthenticatedUserPrincipal;
import com.fluxusbackend.shared.application.security.JwtTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.fluxusbackend.authaccess.domain.model.dto.ForgotPasswordRequest;
import com.fluxusbackend.authaccess.domain.model.dto.VerifyTokenRequest;
import com.fluxusbackend.authaccess.domain.model.dto.ResetPasswordRequest;
import com.fluxusbackend.authaccess.domain.model.dto.NotificationEvent;
import com.fluxusbackend.authaccess.infrastructure.messaging.RabbitMQConfig;
import com.fluxusbackend.authaccess.domain.model.valueobjects.PasswordHash;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth & Access", description = "User registration and login")
public class AuthAccessController {

    private final UserCommandService userCommandService;
    private final UserAuthenticationQueryService userAuthenticationQueryService;
    private final JwtTokenService jwtTokenService;
    private final UserQueryService userQueryService;
    private final com.fluxusbackend.authaccess.infrastructure.persistence.jpa.repositories.UserAccountRepository userAccountRepository;
    private final com.fluxusbackend.authaccess.infrastructure.persistence.jpa.repositories.RoleRepository roleRepository;
    private final com.fluxusbackend.authaccess.infrastructure.persistence.jpa.repositories.RolePermissionRepository rolePermissionRepository;
    private final com.fluxusbackend.authaccess.application.internal.services.AuthorizationService authorizationService;
    private final RabbitTemplate rabbitTemplate;
    private final PasswordEncoder passwordEncoder;

    public AuthAccessController(
            UserCommandService userCommandService,
            UserAuthenticationQueryService userAuthenticationQueryService,
            JwtTokenService jwtTokenService,
            UserQueryService userQueryService,
            com.fluxusbackend.authaccess.infrastructure.persistence.jpa.repositories.UserAccountRepository userAccountRepository,
            com.fluxusbackend.authaccess.infrastructure.persistence.jpa.repositories.RoleRepository roleRepository,
            com.fluxusbackend.authaccess.infrastructure.persistence.jpa.repositories.RolePermissionRepository rolePermissionRepository,
            com.fluxusbackend.authaccess.application.internal.services.AuthorizationService authorizationService,
            RabbitTemplate rabbitTemplate,
            PasswordEncoder passwordEncoder
    ) {
        this.userCommandService = userCommandService;
        this.userAuthenticationQueryService = userAuthenticationQueryService;
        this.jwtTokenService = jwtTokenService;
        this.userQueryService = userQueryService;
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.authorizationService = authorizationService;
        this.rabbitTemplate = rabbitTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered",
                    content = @Content(schema = @Schema(implementation = UserAccountDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content)
    })
    public UserAccountDto register(@Valid @RequestBody RegisterUserCommand command) {
        try {
            var user = userCommandService.handle(command);
            return UserAccountDto.from(user);
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), ex);
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Login user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = AuthenticatedUser.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content)
    })
    public AuthenticatedUser login(@Valid @RequestBody LoginUserQuery query) {
        var user = userAuthenticationQueryService.handle(query);
        
        java.util.List<String> permissions = new java.util.ArrayList<>();
        if (user.getRetailUser().isPresent() && user.getRetailUser().get().getRole() != null) {
            var role = user.getRetailUser().get().getRole();
            permissions = rolePermissionRepository.findByRole(role).stream().map(rp -> rp.getPermission().getDescription()).toList();
        }

        var retail = user.getRetailUser().orElse(null);
        var beneficiary = user.getBeneficiaryUser().orElse(null);
        com.fluxusbackend.authaccess.domain.model.enums.UserActor actor = retail != null ? com.fluxusbackend.authaccess.domain.model.enums.UserActor.RETAIL : com.fluxusbackend.authaccess.domain.model.enums.UserActor.BENEFICIARY;
        com.fluxusbackend.shared.domain.model.valueobjects.CompanyId companyId = retail == null ? null : new com.fluxusbackend.shared.domain.model.valueobjects.CompanyId(retail.getRetailCompanyId());
        Long beneficiaryInstitutionId = beneficiary == null ? null : beneficiary.getBeneficiaryInstitutionId();
        Long roleId = retail == null ? null : retail.getRole().getRoleId();
        String roleName = retail == null ? null : retail.getRole().getName();

        var token = jwtTokenService.generateToken(
                user.getUserId().value(),
                user.getEmail().value(),
                actor,
                companyId,
                beneficiaryInstitutionId,
                roleId,
                roleName,
                permissions
        );
        return new AuthenticatedUser(UserAccountDto.from(user), token);
    }

    @GetMapping("/profile")
    @SecurityRequirement(name = "bearer")
    public Profile profile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            return new Profile(null, null, null, null, null);
        }
        var companyId = principal.companyId();
        Long companyLong = companyId == null ? null : companyId.value();
        return new Profile(
                companyLong,
                principal.beneficiaryInstitutionId(),
                principal.email(),
                principal.actor().name(),
                principal.roleName()
        );
    }

    @GetMapping("/users/{userId}")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Get user account by id (no caller validation)")
    public UserAccountDto getUserById(@PathVariable Long userId) {
        var user = userQueryService.handle(new GetUserByIdQuery(new UserId(userId)))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return UserAccountDto.from(user);
    }

    @GetMapping("/users")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "List users")
    public List<UserAccountDto> listUsers() {
        List<UserAccount> users = userQueryService.findAll();
        var dtos = new ArrayList<UserAccountDto>();
        for (var u : users) dtos.add(UserAccountDto.from(u));
        return dtos;
    }

    @PutMapping("/profile")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Update current user profile")
    public UserAccountDto updateProfile(@Valid @RequestBody UpdateProfilePayload payload) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized access");
        }
        var command = new com.fluxusbackend.authaccess.domain.model.commands.UpdateProfileCommand(
                principal.userId(),
                payload.username(),
                payload.email()
        );
        var user = userCommandService.handle(command);
        return UserAccountDto.from(user);
    }

    public record UpdateProfilePayload(String username, String email) {
    }

    @PutMapping("/change-password")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Change current user password")
    public void changePassword(@Valid @RequestBody ChangePasswordPayload payload) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized access");
        }
        var command = new com.fluxusbackend.authaccess.domain.model.commands.ChangePasswordCommand(
                principal.userId(),
                payload.currentPassword(),
                payload.newPassword()
        );
        userCommandService.handle(command);
    }

    public record ChangePasswordPayload(String currentPassword, String newPassword) {
    }

    @PutMapping("/users/{userId}/role")
    @SecurityRequirement(name = "bearer")
    @PreAuthorize("hasAnyRole('RETAIL_MANAGER', 'RETAIL_FULL_ACCESS')")
    @Operation(summary = "Update user role (MANAGER or FULL_ACCESS only)")
    @Transactional
    public UserAccountDto updateUserRole(@PathVariable Long userId, @Valid @RequestBody UpdateUserRolePayload payload) {
        Long managerCompanyId = authorizationService.getCurrentUserCompanyId().value();

        var user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        var retailUser = user.getRetailUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a retail user"));

        if (!retailUser.getRetailCompanyId().equals(managerCompanyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Manager and target user company mismatch");
        }

        var role = roleRepository.findById(payload.roleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));

        if (!role.getRetailCompanyId().equals(managerCompanyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role does not belong to this company");
        }

        retailUser.updateRole(role);
        var saved = userAccountRepository.save(user);
        return UserAccountDto.from(saved);
    }

    public record UpdateUserRolePayload(Long roleId) {
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Request password reset token")
    @Transactional
    public Map<String, String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        var user = userAccountRepository.findByEmailValue(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado con ese correo electrónico."));

        user.generatePasswordResetToken();
        userAccountRepository.save(user);

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFICATION_EXCHANGE,
                    RabbitMQConfig.RESET_ROUTING_KEY,
                    new NotificationEvent(
                            user.getEmail().value(),
                            "PASSWORD_RECOVERY",
                            null,
                            null,
                            Map.of("token", user.getPasswordResetToken(), "username", user.getUsername())
                    )
            );
        } catch (Exception e) {
            System.err.println("Error publishing password reset event to RabbitMQ: " + e.getMessage());
        }

        return Map.of("message", "Token de recuperación enviado al correo electrónico.");
    }

    @PostMapping("/verify-token")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Verify password reset token")
    public Map<String, String> verifyToken(@Valid @RequestBody VerifyTokenRequest request) {
        var user = userAccountRepository.findByEmailValue(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));

        if (!user.isPasswordResetTokenValid(request.token())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El token es inválido o ha expirado.");
        }

        return Map.of("message", "Token verificado con éxito.");
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Reset password using token")
    @Transactional
    public Map<String, String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        var user = userAccountRepository.findByEmailValue(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));

        if (!user.isPasswordResetTokenValid(request.token())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El token es inválido o ha expirado.");
        }

        var newHash = new PasswordHash(passwordEncoder.encode(request.newPassword()));
        user.updatePassword(newHash);
        user.clearPasswordResetToken();
        userAccountRepository.save(user);

        return Map.of("message", "Contraseña restablecida con éxito.");
    }
}
