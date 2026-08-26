package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;

interface FuelPriceJpaRepository extends JpaRepository<FuelPriceEntity, UUID>, JpaSpecificationExecutor<FuelPriceEntity> {
    @Query("select (count(p) > 0) from FuelPriceEntity p where p.vendorId = :vendorId and p.fuelType = :fuelType " +
            "and p.active = true and p.id <> :excludingId and p.effectiveFrom <= :to " +
            "and (p.effectiveTo is null or p.effectiveTo >= :from)")
    boolean hasOverlap(@Param("vendorId") UUID vendorId, @Param("fuelType") String fuelType,
                       @Param("from") LocalDate from, @Param("to") LocalDate to,
                       @Param("excludingId") UUID excludingId);
}
