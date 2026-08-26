package com.transportlogistics.app.fleet.application.ports.out;

import com.transportlogistics.app.fleet.domain.model.DriverViolation;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DriverViolationRepository {
    DriverViolation save(DriverViolation violation);

    Optional<DriverViolation> findById(UUID id);

    List<DriverViolation> findByDriverId(UUID driverId);

    List<DriverViolation> findByDriverIdAndViolationDateBetween(UUID driverId, OffsetDateTime from, OffsetDateTime to);
}
