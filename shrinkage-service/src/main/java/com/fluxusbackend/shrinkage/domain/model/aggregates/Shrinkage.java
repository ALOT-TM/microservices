package com.fluxusbackend.shrinkage.domain.model.aggregates;

import com.fluxusbackend.shared.domain.model.aggregates.AuditableAggregateRoot;
import com.fluxusbackend.shared.domain.model.aggregates.CompanyScoped;
import com.fluxusbackend.shared.domain.model.valueobjects.CompanyId;
import com.fluxusbackend.shrinkage.domain.model.enums.ShrinkageStatus;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(name = "shrinkage")
@AttributeOverride(name = "id", column = @Column(name = "shrinkage_id", nullable = false, updatable = false))
public class Shrinkage extends AuditableAggregateRoot implements CompanyScoped {

    @Embedded
    private CompanyId companyId;

    @Column(name = "retail_company_headquarter_id", nullable = false)
    private Long retailCompanyHeadquarterId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shrinkage_reason_id", nullable = false)
    private ShrinkageReason shrinkageReason;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "specific_reason", length = 100)
    private String specificReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ShrinkageStatus status;

    @Column(name = "pickup_date")
    private LocalDate pickupDate;

    @Column(name = "shrinkage_value", nullable = false)
    private Double shrinkageValue;

    protected Shrinkage() {
    }

    public Shrinkage(
            Long retailCompanyHeadquarterId,
            Category category,
            ShrinkageReason shrinkageReason,
            String name,
            Integer quantity,
            LocalDate expirationDate,
            String specificReason,
            LocalDate pickupDate,
            Double shrinkageValue
    ) {
        this.retailCompanyHeadquarterId = Objects.requireNonNull(retailCompanyHeadquarterId, "Retail company headquarter id is required");
        this.category = Objects.requireNonNull(category, "Category is required");
        this.shrinkageReason = Objects.requireNonNull(shrinkageReason, "Shrinkage reason is required");
        this.name = Objects.requireNonNull(name, "Name is required");
        this.quantity = Objects.requireNonNull(quantity, "Quantity is required");
        this.expirationDate = expirationDate;
        this.specificReason = specificReason;
        this.pickupDate = pickupDate;
        this.shrinkageValue = Objects.requireNonNull(shrinkageValue, "Shrinkage value is required");
        this.status = ShrinkageStatus.NONE;
    }

    public Double getShrinkageValue() {
        return shrinkageValue;
    }

    public Long getShrinkageId() {
        return getId();
    }

    public Long getRetailCompanyHeadquarterId() {
        return retailCompanyHeadquarterId;
    }

    public Category getCategory() {
        return category;
    }

    public ShrinkageReason getShrinkageReason() {
        return shrinkageReason;
    }

    public String getName() {
        return name;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public String getSpecificReason() {
        return specificReason;
    }

    public ShrinkageStatus getStatus() {
        return status;
    }

    public LocalDate getPickupDate() {
        return pickupDate;
    }

    @Override
    public Optional<CompanyId> getCompanyId() {
        return Optional.ofNullable(companyId);
    }

    @Override
    public void setCompanyId(CompanyId companyId) {
        this.companyId = Objects.requireNonNull(companyId, "Company id is required");
    }

    public void markDonable() {
        if (status != ShrinkageStatus.NONE && status != ShrinkageStatus.NOT_DONABLE && status != ShrinkageStatus.IN_PROCESS) {
            throw new IllegalStateException("Shrinkage must be NONE, NOT_DONABLE or IN_PROCESS before marking donable");
        }
        status = ShrinkageStatus.DONABLE;
    }

    public void markInProcess() {
        if (status == ShrinkageStatus.IN_PROCESS) return;
        if (status != ShrinkageStatus.DONABLE && status != ShrinkageStatus.REQUESTED) {
            throw new IllegalStateException("Shrinkage must be DONABLE or REQUESTED before marking in process");
        }
        status = ShrinkageStatus.IN_PROCESS;
    }

    public void markRequested() {
        if (status == ShrinkageStatus.REQUESTED) return;
        if (status != ShrinkageStatus.DONABLE) {
            throw new IllegalStateException("Shrinkage must be donable before marking requested");
        }
        status = ShrinkageStatus.REQUESTED;
    }

    public void markNotDonable() {
        if (status == ShrinkageStatus.NOT_DONABLE) return;
        if (status != ShrinkageStatus.NONE && status != ShrinkageStatus.DONABLE) {
            throw new IllegalStateException("Shrinkage must be NONE or DONABLE before marking not donable");
        }
        status = ShrinkageStatus.NOT_DONABLE;
    }

    public void markDonated() {
        if (status == ShrinkageStatus.DONATED) return;
        if (status != ShrinkageStatus.DONABLE && status != ShrinkageStatus.REQUESTED && status != ShrinkageStatus.IN_PROCESS) {
            throw new IllegalStateException("Shrinkage must be donable, requested or in process before marking donated");
        }
        status = ShrinkageStatus.DONATED;
    }
}


