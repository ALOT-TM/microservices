package com.fluxusbackend.shrinkage.domain.model.aggregates;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "headquarter_cache")
public class HeadquarterCache {

    @Id
    private Long headquarterId;

    private Long companyId;

    protected HeadquarterCache() {}

    public HeadquarterCache(Long headquarterId, Long companyId) {
        this.headquarterId = headquarterId;
        this.companyId = companyId;
    }

    public Long getHeadquarterId() {
        return headquarterId;
    }

    public Long getCompanyId() {
        return companyId;
    }
}
