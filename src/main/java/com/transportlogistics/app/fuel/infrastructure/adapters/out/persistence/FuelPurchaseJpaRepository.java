package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface FuelPurchaseJpaRepository extends JpaRepository<FuelPurchaseEntity, UUID>, JpaSpecificationExecutor<FuelPurchaseEntity> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from FuelPurchaseEntity p where p.id = :id")
    Optional<FuelPurchaseEntity> findByIdForUpdate(@Param("id") UUID id);
    boolean existsByPurchaseNumber(String purchaseNumber);
    boolean existsByVendorIdAndInvoiceNumberIgnoreCaseAndIdNot(UUID vendorId, String invoiceNumber, UUID id);
}
