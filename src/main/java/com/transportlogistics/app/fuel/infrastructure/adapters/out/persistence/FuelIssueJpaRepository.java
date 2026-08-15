package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface FuelIssueJpaRepository extends JpaRepository<FuelIssueEntity, UUID>, JpaSpecificationExecutor<FuelIssueEntity> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select issue from FuelIssueEntity issue where issue.id = :id")
    Optional<FuelIssueEntity> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByVoucherNumber(String voucherNumber);
}
