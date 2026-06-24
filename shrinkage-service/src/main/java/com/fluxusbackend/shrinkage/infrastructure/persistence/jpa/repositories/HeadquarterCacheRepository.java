package com.fluxusbackend.shrinkage.infrastructure.persistence.jpa.repositories;

import com.fluxusbackend.shrinkage.domain.model.aggregates.HeadquarterCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HeadquarterCacheRepository extends JpaRepository<HeadquarterCache, Long> {
}
