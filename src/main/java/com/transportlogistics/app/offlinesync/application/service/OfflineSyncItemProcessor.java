package com.transportlogistics.app.offlinesync.application.service;

import com.transportlogistics.app.offlinesync.application.ports.in.OfflineSyncUseCase.OperationCommand;
import com.transportlogistics.app.offlinesync.application.ports.out.OfflineOperationHandler;
import com.transportlogistics.app.offlinesync.application.ports.out.OfflineSyncOperationRepository;
import com.transportlogistics.app.offlinesync.domain.model.OfflineHandlerOutcome;
import com.transportlogistics.app.offlinesync.domain.model.OfflineOperationContext;
import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncConflictException;
import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncOperation;
import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncPayloadException;
import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncResult;
import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncResultStatus;
import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncRetryableException;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public class OfflineSyncItemProcessor {
    private final OfflineSyncOperationRepository operations;
    private final Clock clock;

    public OfflineSyncItemProcessor(OfflineSyncOperationRepository operations, Clock clock) {
        this.operations = operations;
        this.clock = clock;
    }

    public OfflineSyncResult process(UUID actorId, String actorName, OperationCommand command, String requestHash,
                                     OfflineOperationHandler handler, OfflineHandlerOutcome predetermined) {
        return operations.findByOperationId(command.operationId())
                .map(existing -> replay(existing, actorId, requestHash, command.aggregateId()))
                .orElseGet(() -> applyFirst(actorId, actorName, command, requestHash, handler, predetermined));
    }

    public OfflineSyncResult replayOnly(UUID actorId, OperationCommand command, String requestHash) {
        return operations.findByOperationId(command.operationId())
                .map(existing -> replay(existing, actorId, requestHash, command.aggregateId()))
                .orElseThrow(() -> new OfflineSyncRetryableException("Concurrent inbox claim did not commit"));
    }

    private OfflineSyncResult applyFirst(UUID actorId, String actorName, OperationCommand command, String requestHash,
                                         OfflineOperationHandler handler, OfflineHandlerOutcome predetermined) {
        OffsetDateTime now = now();
        OfflineSyncResultStatus initialStatus = predetermined == null
                ? OfflineSyncResultStatus.APPLIED : predetermined.status();
        String initialCode = predetermined == null ? null : predetermined.errorCode();
        OfflineSyncOperation inbox = new OfflineSyncOperation(
                command.operationId(), command.operationType(), command.operationVersion(), actorId,
                command.clientInstanceId(), command.aggregateType(), command.aggregateId(), requestHash,
                initialStatus, initialCode, null, now, now);
        operations.claim(inbox);

        if (predetermined != null) {
            return result(command, predetermined.status(), now, predetermined.errorCode(), predetermined.message());
        }

        try {
            OfflineHandlerOutcome outcome = handler.apply(
                    new OfflineOperationContext(command.operationId(), actorId, actorName, command.aggregateId(),
                            command.clientCreatedAt()), command.payload());
            if (outcome.status() != OfflineSyncResultStatus.APPLIED) {
                operations.save(inbox.withResult(outcome.status(), outcome.errorCode(), now));
            }
            return result(command, outcome.status(), now, outcome.errorCode(), outcome.message());
        } catch (OfflineSyncPayloadException exception) {
            operations.save(inbox.withResult(OfflineSyncResultStatus.REJECTED,
                    "OFFLINE_SYNC_PAYLOAD_INVALID", now));
            return result(command, OfflineSyncResultStatus.REJECTED, now,
                    "OFFLINE_SYNC_PAYLOAD_INVALID", safe(exception.getMessage()));
        } catch (OfflineSyncConflictException exception) {
            operations.save(inbox.withResult(OfflineSyncResultStatus.CONFLICT,
                    "OFFLINE_SYNC_CONFLICT", now));
            return result(command, OfflineSyncResultStatus.CONFLICT, now,
                    "OFFLINE_SYNC_CONFLICT", safe(exception.getMessage()));
        } catch (OfflineSyncRetryableException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new OfflineSyncRetryableException("Offline operation could not be processed", exception);
        }
    }

    private OfflineSyncResult replay(OfflineSyncOperation existing, UUID actorId, String requestHash,
                                     UUID requestedAggregateId) {
        OffsetDateTime now = now();
        if (!existing.actorId().equals(actorId) || !existing.requestHash().equals(requestHash)) {
            return new OfflineSyncResult(existing.operationId(), OfflineSyncResultStatus.CONFLICT, now,
                    requestedAggregateId, null, "OFFLINE_SYNC_IDEMPOTENCY_MISMATCH",
                    "Operation identity was already used with different facts");
        }
        if (existing.resultStatus() == OfflineSyncResultStatus.APPLIED) {
            return new OfflineSyncResult(existing.operationId(), OfflineSyncResultStatus.ALREADY_APPLIED, now,
                    existing.aggregateId(), null, null, "Operation was already applied");
        }
        return new OfflineSyncResult(existing.operationId(), existing.resultStatus(), now,
                existing.aggregateId(), existing.resultVersion(), existing.resultCode(),
                existing.resultStatus() == OfflineSyncResultStatus.CONFLICT
                        ? "Operation conflict was already recorded" : "Operation rejection was already recorded");
    }

    private OfflineSyncResult result(OperationCommand command, OfflineSyncResultStatus status,
                                     OffsetDateTime now, String code, String message) {
        return new OfflineSyncResult(command.operationId(), status, now, command.aggregateId(), null, code, safe(message));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String sanitized = value.replace('\n', ' ').replace('\r', ' ').trim();
        return sanitized.substring(0, Math.min(500, sanitized.length()));
    }
}
