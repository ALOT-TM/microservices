package com.fluxusbackend.authaccess.domain.model.dto;

public record AuthenticatedUser(UserAccountDto user, String token) {
}
