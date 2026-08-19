package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

interface DriverViolationJpaRepository extends JpaRepository<DriverViolationEntity, UUID> {

    List<DriverViolationEntity> findByDriverIdOrderByViolationDateDesc(UUID driverId);

    List<DriverViolationEntity> findByDriverIdAndViolationDateBetweenOrderByViolationDateDesc(
            UUID driverId,
            OffsetDateTime from,
            OffsetDateTime to
    );
}
