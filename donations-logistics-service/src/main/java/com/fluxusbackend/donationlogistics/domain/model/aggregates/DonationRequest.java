package com.fluxusbackend.donationlogistics.domain.model.aggregates;

import com.fluxusbackend.donationlogistics.domain.model.enums.DonationRequestStatus;
import com.fluxusbackend.donationlogistics.domain.model.valueobjects.BeneficiaryReferenceId;
import com.fluxusbackend.donationlogistics.domain.model.valueobjects.ShrinkageReferenceId;
import com.fluxusbackend.donationlogistics.domain.model.valueobjects.DonationRequestId;
import com.fluxusbackend.donationlogistics.domain.model.valueobjects.PickupConfirmationDate;
import com.fluxusbackend.shared.domain.model.aggregates.AuditableAggregateRoot;
import com.fluxusbackend.shared.domain.model.valueobjects.CompanyId;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "donation_requests")
public class DonationRequest extends AuditableAggregateRoot {

    @Embedded
    private ShrinkageReferenceId shrinkageReferenceId;

    @Embedded
    private BeneficiaryReferenceId beneficiaryReferenceId;

    @Embedded
    private CompanyId companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DonationRequestStatus status;

    @Column(name = "notes", length = 500)
    private String notes;

    @Embedded
    private PickupConfirmationDate pickupConfirmationDate;

    @Column(name = "pickup_confirmation_comment", length = 250)
    private String pickupComment;

    protected DonationRequest() {
    }

    public DonationRequest(
            ShrinkageReferenceId shrinkageReferenceId,
            BeneficiaryReferenceId beneficiaryReferenceId,
            CompanyId companyId,
            String notes
    ) {
        this.shrinkageReferenceId = Objects.requireNonNull(shrinkageReferenceId, "Shrinkage reference is required");
        this.beneficiaryReferenceId = Objects.requireNonNull(beneficiaryReferenceId, "Beneficiary reference is required");
        this.companyId = Objects.requireNonNull(companyId, "Company ID is required");
        this.notes = notes;
        this.status = DonationRequestStatus.PENDING;
    }

    public DonationRequestId getDonationRequestId() {
        return new DonationRequestId(getId());
    }

    public ShrinkageReferenceId getShrinkageReferenceId() {
        return shrinkageReferenceId;
    }

    public BeneficiaryReferenceId getBeneficiaryReferenceId() {
        return beneficiaryReferenceId;
    }

    public CompanyId getCompanyId() {
        return companyId;
    }

    public DonationRequestStatus getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public java.util.Optional<PickupConfirmationDate> getPickupConfirmationDate() {
        return java.util.Optional.ofNullable(pickupConfirmationDate);
    }

    public java.util.Optional<String> getPickupComment() {
        return java.util.Optional.ofNullable(pickupComment);
    }

    public void accept() {
        if (status == DonationRequestStatus.ACCEPTED) return;
        if (status != DonationRequestStatus.PENDING) {
            throw new IllegalStateException("Only pending donation requests can be accepted");
        }
        status = DonationRequestStatus.ACCEPTED;
    }

    public void reject() {
        if (status == DonationRequestStatus.REJECTED) return;
        if (status != DonationRequestStatus.PENDING) {
            throw new IllegalStateException("Only pending donation requests can be rejected");
        }
        status = DonationRequestStatus.REJECTED;
    }

    public void cancel() {
        if (status == DonationRequestStatus.COMPLETED || status == DonationRequestStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel completed or already cancelled donation requests");
        }
        status = DonationRequestStatus.CANCELLED;
    }

    public void complete() {
        if (status != DonationRequestStatus.ACCEPTED) {
            throw new IllegalStateException("Only accepted donation requests can be completed");
        }
        status = DonationRequestStatus.COMPLETED;
    }

    public void confirmPickup(PickupConfirmationDate pickupConfirmationDate, String comment) {
        if (this.status == DonationRequestStatus.COMPLETED) return;
        if (this.status != DonationRequestStatus.ACCEPTED) {
            throw new IllegalStateException("Only accepted donation requests can be completed");
        }
        this.pickupConfirmationDate = Objects.requireNonNull(pickupConfirmationDate, "Pickup confirmation date is required");
        this.pickupComment = comment;
        this.status = DonationRequestStatus.COMPLETED;
    }
}

