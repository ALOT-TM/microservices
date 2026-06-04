package com.fluxusbackend.authaccess.domain.model.aggregates;

import com.fluxusbackend.shared.domain.model.aggregates.AuditableAggregateRoot;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "role")
@AttributeOverride(name = "id", column = @Column(name = "role_id", nullable = false, updatable = false))
public class Role extends AuditableAggregateRoot {

    @Column(name = "retail_company_id", nullable = false)
    private Long retailCompanyId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    protected Role() {
    }

    public Role(Long retailCompanyId, String name) {
        this.retailCompanyId = Objects.requireNonNull(retailCompanyId, "Retail company id is required");
        this.name = Objects.requireNonNull(name, "Role name is required");
    }

    public Long getRoleId() {
        return getId();
    }

    public Long getRetailCompanyId() {
        return retailCompanyId;
    }

    public String getName() {
        return name;
    }

    public void rename(String name) {
        this.name = Objects.requireNonNull(name, "Name is required");
    }
}
