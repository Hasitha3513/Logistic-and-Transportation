package com.transportlogistics.app.fleet.application.ports.in;

import com.transportlogistics.app.fleet.domain.model.DriverException;
import com.transportlogistics.app.fleet.domain.model.DriverExceptionStatus;
import com.transportlogistics.app.fleet.domain.model.DriverExceptionType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface DriverExceptionUseCase {

    DriverException create(UUID driverId, CreateCommand command, String actor);

    DriverException get(UUID driverId, UUID exceptionId);

    List<DriverException> list(UUID driverId);

    DriverException update(UUID driverId, UUID exceptionId, UpdateCommand command, String actor);

    DriverException cancel(UUID driverId, UUID exceptionId, String remarks, String actor);

    DriverException complete(UUID driverId, UUID exceptionId, String remarks, String actor);

    boolean hasOverlappingException(UUID driverId, OffsetDateTime from, OffsetDateTime to);

    record CreateCommand(
            DriverExceptionType exceptionType,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            String reason,
            String remarks
    ) {
    }

    record UpdateCommand(
            DriverExceptionType exceptionType,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            DriverExceptionStatus status,
            String reason,
            String remarks
    ) {
    }
}
