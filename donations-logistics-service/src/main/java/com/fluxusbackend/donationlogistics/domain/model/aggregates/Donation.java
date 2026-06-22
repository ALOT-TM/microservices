package com.fluxusbackend.donationlogistics.domain.model.aggregates;

import com.fluxusbackend.donationlogistics.domain.model.enums.DonationItemStatus;
import com.fluxusbackend.donationlogistics.domain.model.enums.DonationStatus;
import com.fluxusbackend.donationlogistics.domain.model.valueobjects.BeneficiaryReferenceId;
import com.fluxusbackend.donationlogistics.domain.model.valueobjects.PickupDate;
import com.fluxusbackend.donationlogistics.domain.model.valueobjects.DonationId;
import com.fluxusbackend.donationlogistics.domain.model.valueobjects.DonationQuantity;
import com.fluxusbackend.donationlogistics.domain.model.valueobjects.ShrinkageReferenceId;
import com.fluxusbackend.donationlogistics.domain.model.valueobjects.PickupConfirmationDate;
import com.fluxusbackend.donationlogistics.domain.model.valueobjects.ScheduledPickupDate;
import com.fluxusbackend.shared.domain.model.aggregates.AuditableAggregateRoot;
import com.fluxusbackend.shared.domain.model.aggregates.CompanyScoped;
import com.fluxusbackend.shared.domain.model.valueobjects.CompanyId;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(name = "donations")
public class Donation extends AuditableAggregateRoot implements CompanyScoped {

    @OneToMany(mappedBy = "donation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DonationItem> items = new ArrayList<>();

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "beneficiary_institution_id", nullable = false))
    private BeneficiaryReferenceId beneficiaryReferenceId;

    @Embedded
    private DonationQuantity quantity;

    @Embedded
    private ScheduledPickupDate scheduledPickupDate;

    @Embedded
    private PickupDate pickupDate;

    @Embedded
    private PickupConfirmationDate pickupConfirmationDate;

    @Column(name = "pickup_confirmation_comment", length = 250)
    private String pickupComment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DonationStatus status;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Embedded
    private CompanyId companyId;

    protected Donation() {
    }

    public Donation(
            ShrinkageReferenceId shrinkageReferenceId,
            BeneficiaryReferenceId beneficiaryReferenceId,
            DonationQuantity quantity,
            ScheduledPickupDate scheduledPickupDate
    ) {
        this.beneficiaryReferenceId = Objects.requireNonNull(beneficiaryReferenceId, "Beneficiary reference id is required");
        this.quantity = Objects.requireNonNull(quantity, "Donation quantity is required");
        this.scheduledPickupDate = Objects.requireNonNull(scheduledPickupDate, "Scheduled pickup date is required");
        this.status = DonationStatus.ASSIGNED;
        this.items.add(new DonationItem(this, shrinkageReferenceId, DonationItemStatus.ASSIGNED));
    }

    public DonationId getDonationId() {
        return new DonationId(getId());
    }

    public List<DonationItem> getItems() {
        return items;
    }

    public ShrinkageReferenceId getShrinkageReferenceId() {
        return items.isEmpty() ? null : items.get(0).getShrinkageReferenceId();
    }

    public BeneficiaryReferenceId getBeneficiaryReferenceId() {
        return beneficiaryReferenceId;
    }

    public DonationQuantity getQuantity() {
        return quantity;
    }

    public ScheduledPickupDate getScheduledPickupDate() {
        return scheduledPickupDate;
    }

    public Optional<PickupDate> getPickupDate() {
        return Optional.ofNullable(pickupDate);
    }

    public Optional<PickupConfirmationDate> getPickupConfirmationDate() {
        return Optional.ofNullable(pickupConfirmationDate);
    }

    public Optional<String> getPickupComment() {
        return Optional.ofNullable(pickupComment);
    }

    public DonationStatus getStatus() {
        return status;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public java.util.Optional<CompanyId> getCompanyId() {
        return Optional.ofNullable(companyId);
    }

    public void setCompanyId(CompanyId companyId) {
        this.companyId = companyId;
    }

    public void markPendingPickup(PickupDate pickupDate) {
        if (status != DonationStatus.ASSIGNED) {
            throw new IllegalStateException("Donation must be assigned before pickup");
        }
        this.pickupDate = Objects.requireNonNull(pickupDate, "Pickup date is required");
        this.status = DonationStatus.PENDING_PICKUP;
        for (var item : items) {
            item.setStatus(DonationItemStatus.PENDING_PICKUP);
        }
    }

    public void confirmPickup(PickupConfirmationDate pickupConfirmationDate, Optional<String> comment) {
        if (this.status == DonationStatus.PICKED_UP) return;
        if (status != DonationStatus.PENDING_PICKUP && status != DonationStatus.ASSIGNED) {
            throw new IllegalStateException("Donation must be pending pickup or assigned before confirmation");
        }
        this.pickupConfirmationDate = Objects.requireNonNull(pickupConfirmationDate, "Pickup confirmation date is required");
        this.pickupComment = comment == null ? null : comment.orElse(null);
        this.status = DonationStatus.PICKED_UP;
        this.completedAt = Instant.now();
        for (var item : items) {
            item.setStatus(DonationItemStatus.PICKED_UP);
        }
    }
}


