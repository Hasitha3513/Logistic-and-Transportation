package com.transportlogistics.app.freight.exception.ports.inbound;

import com.transportlogistics.app.freight.exception.domain.CargoException;
import com.transportlogistics.app.freight.exception.domain.ExceptionSeverity;
import com.transportlogistics.app.freight.exception.domain.ExceptionStatus;
import com.transportlogistics.app.freight.exception.domain.ExceptionType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Inbound port (Use Case) for Cargo Exception management (US-30).
 */
public interface CargoExceptionUseCase {

    // ── Commands ──────────────────────────────────────────────────────────────

    record RecordExceptionCommand(
            ExceptionType exceptionType,
            ExceptionSeverity severity,
            UUID freightOrderId,
            UUID manifestId,
            UUID manifestItemId,
            String description,
            String impact,
            String restriction,
            String correctiveAction
    ) {}

    record HoldExceptionCommand(
            String restriction,
            String reason,
            long version
    ) {}

    record EscalateExceptionCommand(
            String reason,
            long version
    ) {}

    record ReleaseExceptionCommand(
            String reason,
            long version
    ) {}

    record RejectExceptionCommand(
            String reason,
            long version
    ) {}

    record ResolveExceptionCommand(
            String resolution,
            String correctiveAction,
            String reason,
            long version
    ) {}

    // ── Operations ────────────────────────────────────────────────────────────

    CargoException record(RecordExceptionCommand command, String actor);

    CargoException hold(UUID id, HoldExceptionCommand command, String actor);

    CargoException escalate(UUID id, EscalateExceptionCommand command, String actor);

    CargoException release(UUID id, ReleaseExceptionCommand command, String actor);

    CargoException reject(UUID id, RejectExceptionCommand command, String actor);

    CargoException resolve(UUID id, ResolveExceptionCommand command, String actor);

    // ── Queries ───────────────────────────────────────────────────────────────

    CargoException get(UUID id);

    List<CargoException> list(UUID freightOrderId,
                              UUID manifestId,
                              ExceptionType type,
                              ExceptionStatus status,
                              int page,
                              int size);
}
