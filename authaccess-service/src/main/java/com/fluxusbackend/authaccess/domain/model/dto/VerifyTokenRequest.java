package com.fluxusbackend.authaccess.domain.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerifyTokenRequest(
    @NotBlank @Email String email,
    @NotBlank String token
) {}
