package com.fluxusbackend.authaccess.domain.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @NotBlank @Email String email,
    @NotBlank String token,
    @NotBlank @Size(min = 6) String newPassword
) {}
