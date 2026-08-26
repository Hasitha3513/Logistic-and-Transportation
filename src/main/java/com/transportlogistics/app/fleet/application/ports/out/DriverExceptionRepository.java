package com.transportlogistics.app.fleet.application.ports.out;

import com.transportlogistics.app.fleet.domain.model.DriverException;
import com.transportlogistics.app.fleet.domain.model.DriverExceptionStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DriverExceptionRepository {

    DriverException save(DriverException exception);

    Optional<DriverException> findById(UUID id);

    List<DriverException> findByDriverId(UUID driverId);

    boolean hasOverlappingException(UUID driverId, OffsetDateTime from, OffsetDateTime to, List<DriverExceptionStatus> blockingStatuses);

    boolean hasOverlappingExceptionExcluding(UUID driverId, OffsetDateTime from, OffsetDateTime to, List<DriverExceptionStatus> blockingStatuses, UUID excludeExceptionId);
}
