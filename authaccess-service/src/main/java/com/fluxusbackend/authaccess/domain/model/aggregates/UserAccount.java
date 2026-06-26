package com.fluxusbackend.authaccess.domain.model.aggregates;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fluxusbackend.authaccess.domain.model.valueobjects.EmailAddress;
import com.fluxusbackend.authaccess.domain.model.valueobjects.PasswordHash;
import com.fluxusbackend.authaccess.domain.model.valueobjects.UserId;
import com.fluxusbackend.shared.domain.model.aggregates.AuditableAggregateRoot;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(name = "user_account")
@AttributeOverride(name = "id", column = @Column(name = "user_account_id", nullable = false, updatable = false))
public class UserAccount extends AuditableAggregateRoot {

    @Embedded
    private EmailAddress email;

    @Embedded
    private PasswordHash passwordHash;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "password_reset_token", length = 100)
    private String passwordResetToken;

    @Column(name = "password_reset_token_expires_at")
    private java.time.LocalDateTime passwordResetTokenExpiresAt;

    @OneToOne(mappedBy = "userAccount", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private RetailUser retailUser;

    @OneToOne(mappedBy = "userAccount", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private BeneficiaryUser beneficiaryUser;

    protected UserAccount() {
    }

    public UserAccount(EmailAddress email, PasswordHash passwordHash, String username) {
        this.email = Objects.requireNonNull(email, "Email is required");
        this.passwordHash = Objects.requireNonNull(passwordHash, "Password hash is required");
        this.username = Objects.requireNonNull(username, "Username is required");
    }

    public UserId getUserId() {
        return new UserId(getId());
    }

    public EmailAddress getEmail() {
        return email;
    }

    @JsonIgnore
    public PasswordHash getPasswordHash() {
        return passwordHash;
    }

    public String getUsername() {
        return username;
    }

    public Optional<RetailUser> getRetailUser() {
        return Optional.ofNullable(retailUser);
    }

    public Optional<BeneficiaryUser> getBeneficiaryUser() {
        return Optional.ofNullable(beneficiaryUser);
    }

    public void attachRetailUser(RetailUser retailUser) {
        this.retailUser = Objects.requireNonNull(retailUser, "Retail user is required");
    }

    public void attachBeneficiaryUser(BeneficiaryUser beneficiaryUser) {
        this.beneficiaryUser = Objects.requireNonNull(beneficiaryUser, "Beneficiary user is required");
    }

    public void updateProfile(String username, EmailAddress email) {
        this.username = Objects.requireNonNull(username, "Username is required");
        this.email = Objects.requireNonNull(email, "Email is required");
    }

    public void updatePassword(PasswordHash passwordHash) {
        this.passwordHash = Objects.requireNonNull(passwordHash, "Password hash is required");
    }

    public String getPasswordResetToken() {
        return passwordResetToken;
    }

    public java.time.LocalDateTime getPasswordResetTokenExpiresAt() {
        return passwordResetTokenExpiresAt;
    }

    public void generatePasswordResetToken() {
        this.passwordResetToken = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        this.passwordResetTokenExpiresAt = java.time.LocalDateTime.now().plusMinutes(10);
    }

    public void clearPasswordResetToken() {
        this.passwordResetToken = null;
        this.passwordResetTokenExpiresAt = null;
    }

    public boolean isPasswordResetTokenValid(String token) {
        if (this.passwordResetToken == null || !this.passwordResetToken.equals(token)) {
            return false;
        }
        return this.passwordResetTokenExpiresAt != null && this.passwordResetTokenExpiresAt.isAfter(java.time.LocalDateTime.now());
    }
}
