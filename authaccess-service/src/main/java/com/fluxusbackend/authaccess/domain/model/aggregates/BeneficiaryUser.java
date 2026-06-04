package com.fluxusbackend.authaccess.domain.model.aggregates;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "beneficiary_user")
public class BeneficiaryUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "beneficiary_user_id", nullable = false, updatable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_account_id", nullable = false, unique = true)
    private UserAccount userAccount;

    @Column(name = "beneficiary_institution_id", nullable = false)
    private Long beneficiaryInstitutionId;

    protected BeneficiaryUser() {
    }

    public BeneficiaryUser(UserAccount userAccount, Long beneficiaryInstitutionId) {
        this.userAccount = Objects.requireNonNull(userAccount, "User account is required");
        this.beneficiaryInstitutionId = Objects.requireNonNull(beneficiaryInstitutionId, "Beneficiary institution id is required");
    }

    public Long getId() {
        return id;
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }

    public Long getBeneficiaryInstitutionId() {
        return beneficiaryInstitutionId;
    }
}
