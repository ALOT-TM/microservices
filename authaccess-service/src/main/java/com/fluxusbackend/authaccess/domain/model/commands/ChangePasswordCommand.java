package com.fluxusbackend.authaccess.domain.model.commands;

import java.util.Objects;

public record ChangePasswordCommand(Long userId, String currentPassword, String newPassword) {
    public ChangePasswordCommand {
        Objects.requireNonNull(userId, "User id is required");
        Objects.requireNonNull(currentPassword, "Current password is required");
        Objects.requireNonNull(newPassword, "New password is required");
        if (currentPassword.isBlank()) {
            throw new IllegalArgumentException("Current password cannot be empty");
        }
        if (newPassword.isBlank()) {
            throw new IllegalArgumentException("New password cannot be empty");
        }
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters");
        }
        if (!newPassword.matches(".*[^a-zA-Z0-9].*")) {
            throw new IllegalArgumentException("New password must contain at least one special character");
        }
    }
}
