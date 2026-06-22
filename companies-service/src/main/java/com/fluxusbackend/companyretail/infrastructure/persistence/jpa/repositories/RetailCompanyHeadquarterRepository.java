package com.fluxusbackend.companyretail.infrastructure.persistence.jpa.repositories;

import com.fluxusbackend.companyretail.domain.model.aggregates.RetailCompanyHeadquarter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RetailCompanyHeadquarterRepository extends JpaRepository<RetailCompanyHeadquarter, Long> {
    @Query("select h from RetailCompanyHeadquarter h where h.retailCompany.id = :companyId")
    List<RetailCompanyHeadquarter> findByCompanyId(@Param("companyId") Long companyId);
}
