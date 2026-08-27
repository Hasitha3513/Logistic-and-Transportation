package com.transportlogistics.app.freight.exception.application;

import com.transportlogistics.app.freight.exception.domain.CargoException;
import com.transportlogistics.app.freight.exception.domain.ExceptionSeverity;
import com.transportlogistics.app.freight.exception.domain.ExceptionStatus;
import com.transportlogistics.app.freight.exception.domain.ExceptionType;
import com.transportlogistics.app.freight.exception.ports.inbound.CargoExceptionUseCase;
import com.transportlogistics.app.freight.exception.ports.outbound.CargoExceptionNumberGenerator;
import com.transportlogistics.app.freight.exception.ports.outbound.CargoExceptionRepository;
import com.transportlogistics.app.freight.exception.ports.outbound.CargoExceptionTransaction;
import com.transportlogistics.app.freight.order.ports.inbound.FreightOrderLookup;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Application-level orchestration for CargoException use cases (US-30).
 * Framework-free; depends only on domain types and outbound ports.
 */
public final class CargoExceptionService implements CargoExceptionUseCase {

    private final CargoExceptionRepository repository;
    private final CargoExceptionNumberGenerator numberGenerator;
    private final CargoExceptionTransaction transactions;
    private final FreightOrderLookup freightOrderLookup;
    private final Clock clock;

    public CargoExceptionService(CargoExceptionRepository repository,
                                 CargoExceptionNumberGenerator numberGenerator,
                                 CargoExceptionTransaction transactions,
                                 FreightOrderLookup freightOrderLookup,
                                 Clock clock) {
        this.repository       = Objects.requireNonNull(repository,       "repository is required");
        this.numberGenerator  = Objects.requireNonNull(numberGenerator,  "numberGenerator is required");
        this.transactions     = Objects.requireNonNull(transactions,     "transactions is required");
        this.freightOrderLookup = Objects.requireNonNull(freightOrderLookup, "freightOrderLookup is required");
        this.clock            = Objects.requireNonNull(clock,            "clock is required");
    }

    // ── record ────────────────────────────────────────────────────────────────

    @Override
    public CargoException record(RecordExceptionCommand command, String actor) {
        return transactions.execute(() -> {
            requireActor(actor);
            Objects.requireNonNull(command, "command is required");
            Objects.requireNonNull(command.freightOrderId(), "freightOrderId is required");
            Objects.requireNonNull(command.exceptionType(), "exceptionType is required");

            // Verify freight order exists (cross-module via inbound port — no JPA join)
            freightOrderLookup.find(command.freightOrderId())
                    .orElseThrow(() -> new NotFoundException(
                            "FREIGHT_ORDER_NOT_FOUND",
                            "Freight order not found: " + command.freightOrderId()));

            OffsetDateTime now = OffsetDateTime.now(clock);
            String exceptionNumber = numberGenerator.nextExceptionNumber();
            ExceptionSeverity severity = command.severity() != null ? command.severity() : ExceptionSeverity.MEDIUM;

            CargoException exception = new CargoException(
                    UUID.randomUUID(),
                    exceptionNumber,
                    command.exceptionType(),
                    ExceptionStatus.OPEN,
                    severity,
                    command.freightOrderId(),
                    command.manifestId(),
                    command.manifestItemId(),
                    command.description(),
                    command.impact(),
                    command.restriction(),
                    command.correctiveAction(),
                    null,
                    null,
                    null,
                    List.of(),
                    now,
                    now,
                    actor,
                    actor,
                    0L
            );

            return repository.save(exception);
        });
    }

    // ── hold ──────────────────────────────────────────────────────────────────

    @Override
    public CargoException hold(UUID id, HoldExceptionCommand command, String actor) {
        return transactions.execute(() -> {
            requireActor(actor);
            CargoException exception = findOrThrow(id);
            requireVersionMatch(exception, command.version());
            CargoException held = exception.hold(command.restriction(), command.reason(), actor, OffsetDateTime.now(clock));
            return repository.save(held);
        });
    }

    // ── escalate ──────────────────────────────────────────────────────────────

    @Override
    public CargoException escalate(UUID id, EscalateExceptionCommand command, String actor) {
        return transactions.execute(() -> {
            requireActor(actor);
            CargoException exception = findOrThrow(id);
            requireVersionMatch(exception, command.version());
            CargoException escalated = exception.escalate(command.reason(), actor, OffsetDateTime.now(clock));
            return repository.save(escalated);
        });
    }

    // ── release ───────────────────────────────────────────────────────────────

    @Override
    public CargoException release(UUID id, ReleaseExceptionCommand command, String actor) {
        return transactions.execute(() -> {
            requireActor(actor);
            CargoException exception = findOrThrow(id);
            requireVersionMatch(exception, command.version());
            CargoException released = exception.release(command.reason(), actor, OffsetDateTime.now(clock));
            return repository.save(released);
        });
    }

    // ── reject ────────────────────────────────────────────────────────────────

    @Override
    public CargoException reject(UUID id, RejectExceptionCommand command, String actor) {
        return transactions.execute(() -> {
            requireActor(actor);
            CargoException exception = findOrThrow(id);
            requireVersionMatch(exception, command.version());
            CargoException rejected = exception.reject(command.reason(), actor, OffsetDateTime.now(clock));
            return repository.save(rejected);
        });
    }

    // ── resolve ───────────────────────────────────────────────────────────────

    @Override
    public CargoException resolve(UUID id, ResolveExceptionCommand command, String actor) {
        return transactions.execute(() -> {
            requireActor(actor);
            CargoException exception = findOrThrow(id);
            requireVersionMatch(exception, command.version());
            CargoException resolved = exception.resolve(
                    command.resolution(), command.correctiveAction(), command.reason(),
                    actor, OffsetDateTime.now(clock));
            return repository.save(resolved);
        });
    }

    // ── get ───────────────────────────────────────────────────────────────────

    @Override
    public CargoException get(UUID id) {
        return findOrThrow(id);
    }

    // ── list ──────────────────────────────────────────────────────────────────

    @Override
    public List<CargoException> list(UUID freightOrderId,
                                     UUID manifestId,
                                     ExceptionType type,
                                     ExceptionStatus status,
                                     int page,
                                     int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 || size > 100 ? 20 : size;
        return repository.findAll(freightOrderId, manifestId, type, status, safePage, safeSize);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private CargoException findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "CARGO_EXCEPTION_NOT_FOUND",
                        "Cargo exception not found: " + id));
    }

    private void requireActor(String actor) {
        if (actor == null || actor.isBlank()) {
            throw new BusinessRuleException("ACTOR_REQUIRED", "Actor is required");
        }
    }

    private void requireVersionMatch(CargoException exception, long requestedVersion) {
        if (exception.getVersion() != requestedVersion) {
            throw new ConflictException(
                    "CARGO_EXCEPTION_STALE_VERSION",
                    "Stale version: provided " + requestedVersion
                            + " but current is " + exception.getVersion());
        }
    }
}
