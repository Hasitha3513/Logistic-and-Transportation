package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.util.List;

interface FuelIssueJpaRepository extends JpaRepository<FuelIssueEntity, UUID>, JpaSpecificationExecutor<FuelIssueEntity> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select issue from FuelIssueEntity issue where issue.id = :id")
    Optional<FuelIssueEntity> findByIdForUpdate(@Param("id") UUID id);

    List<FuelIssueEntity> findByTripIdOrderByIssueDateTimeAsc(UUID tripId);

    @Query("select issue from FuelIssueEntity issue where issue.status = 'ISSUED' " +
            "and issue.issueDateTime >= :from and issue.issueDateTime < :to order by issue.issueDateTime, issue.id")
    List<FuelIssueEntity> findIssuedBetween(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to,
                                            Pageable pageable);

    boolean existsByVoucherNumber(String voucherNumber);
}
