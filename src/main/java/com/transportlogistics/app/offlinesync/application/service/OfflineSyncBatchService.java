package com.transportlogistics.app.offlinesync.application.service;

import com.transportlogistics.app.offlinesync.application.ports.in.OfflineSyncUseCase;
import com.transportlogistics.app.offlinesync.application.ports.out.OfflineOperationHandler;
import com.transportlogistics.app.offlinesync.application.ports.out.OfflineRequestHasher;
import com.transportlogistics.app.offlinesync.application.ports.out.OfflineSyncActorDirectory;
import com.transportlogistics.app.offlinesync.application.ports.out.OfflineSyncItemTransaction;
import com.transportlogistics.app.offlinesync.domain.model.OfflineAggregateType;
import com.transportlogistics.app.offlinesync.domain.model.OfflineHandlerOutcome;
import com.transportlogistics.app.offlinesync.domain.model.OfflineOperationType;
import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncDuplicateClaimException;
import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncResult;
import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncResultStatus;
import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncRetryableException;
import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class OfflineSyncBatchService implements OfflineSyncUseCase {
    public static final int MAX_BATCH_SIZE = 50;

    private final OfflineSyncActorDirectory actors;
    private final OfflineOperationHandlerRegistry handlers;
    private final OfflineRequestHasher requestHasher;
    private final OfflineSyncItemTransaction transactions;
    private final OfflineSyncItemProcessor processor;
    private final Clock clock;

    public OfflineSyncBatchService(OfflineSyncActorDirectory actors,
                                   OfflineOperationHandlerRegistry handlers,
                                   OfflineRequestHasher requestHasher,
                                   OfflineSyncItemTransaction transactions,
                                   OfflineSyncItemProcessor processor,
                                   Clock clock) {
        this.actors = actors;
        this.handlers = handlers;
        this.requestHasher = requestHasher;
        this.transactions = transactions;
        this.processor = processor;
        this.clock = clock;
    }

    @Override
    public BatchResult synchronize(BatchCommand command) {
        if (command.operations() == null || command.operations().isEmpty()) {
            throw new BusinessRuleException("OFFLINE_SYNC_BATCH_INVALID", "Batch must contain at least one operation");
        }
        if (command.operations().size() > MAX_BATCH_SIZE) {
            throw new BusinessRuleException("OFFLINE_SYNC_BATCH_TOO_LARGE", "Batch cannot exceed 50 operations");
        }
        OfflineSyncActorDirectory.Actor actor = actors.findByUsername(command.username())
                .orElseThrow(() -> new BusinessRuleException("OFFLINE_SYNC_ACTOR_INVALID",
                        "Authenticated actor could not be resolved"));

        List<OfflineSyncResult> results = new ArrayList<>(command.operations().size());
        for (OperationCommand operation : command.operations()) {
            results.add(processOne(actor, command.authorities(), operation));
        }
        return new BatchResult(now(), results);
    }

    private OfflineSyncResult processOne(OfflineSyncActorDirectory.Actor actor, Set<String> authorities,
                                         OperationCommand operation) {
        OfflineHandlerOutcome invalid = validateEnvelope(operation);
        if (invalid != null) {
            return directResult(operation, invalid);
        }

        String requestHash = requestHasher.hash(operation);
        OfflineOperationHandler handler = null;
        OfflineHandlerOutcome predetermined = null;

        if (!OfflineOperationType.supports(operation.operationType())) {
            predetermined = OfflineHandlerOutcome.rejected("OFFLINE_SYNC_OPERATION_UNSUPPORTED",
                    "Operation type is not supported");
        } else if (operation.operationVersion() != 1) {
            predetermined = OfflineHandlerOutcome.rejected("OFFLINE_SYNC_PAYLOAD_VERSION_UNSUPPORTED",
                    "Operation version is not supported");
        } else {
            handler = handlers.find(operation.operationType(), operation.operationVersion()).orElse(null);
            if (handler == null) {
                predetermined = OfflineHandlerOutcome.rejected("OFFLINE_SYNC_OPERATION_UNSUPPORTED",
                        "Operation handler is not available");
            } else if (!handler.isAuthorized(authorities)) {
                return directResult(operation, OfflineHandlerOutcome.rejected("OFFLINE_SYNC_FORBIDDEN",
                        "Current actor is not permitted to apply this operation"));
            }
        }

        OfflineOperationHandler selectedHandler = handler;
        OfflineHandlerOutcome selectedOutcome = predetermined;
        try {
            return transactions.execute(() -> processor.process(actor.id(), actor.username(), operation, requestHash,
                    selectedHandler, selectedOutcome));
        } catch (OfflineSyncDuplicateClaimException exception) {
            try {
                return transactions.execute(() -> processor.replayOnly(actor.id(), operation, requestHash));
            } catch (OfflineSyncRetryableException replayFailure) {
                return retryable(operation);
            }
        } catch (OfflineSyncRetryableException exception) {
            return retryable(operation);
        }
    }

    private OfflineHandlerOutcome validateEnvelope(OperationCommand operation) {
        if (operation.operationId() == null || operation.aggregateId() == null
                || operation.clientInstanceId() == null || operation.payload() == null
                || operation.payload().isNull() || operation.clientCreatedAt() == null
                || operation.clientUpdatedAt() == null || operation.operationType() == null
                || operation.operationType().isBlank() || operation.aggregateType() == null
                || operation.aggregateType().isBlank() || operation.operationVersion() <= 0) {
            return OfflineHandlerOutcome.rejected("OFFLINE_SYNC_PAYLOAD_INVALID", "Operation envelope is invalid");
        }
        if (!OfflineAggregateType.supports(operation.aggregateType()) || operation.baseVersion() != null) {
            return OfflineHandlerOutcome.rejected("OFFLINE_SYNC_PAYLOAD_INVALID", "Aggregate or base version is invalid");
        }
        String canonicalOperationId = operation.operationId().toString();
        if (operation.idempotencyKey() == null || !canonicalOperationId.equals(operation.idempotencyKey())) {
            return OfflineHandlerOutcome.rejected("OFFLINE_SYNC_PAYLOAD_INVALID",
                    "Idempotency key must equal the canonical operation ID");
        }
        boolean aggregateMatches = operation.operationType().equals("VEHICLE_READING_RECORD")
                ? operation.aggregateType().equals("VEHICLE") : operation.aggregateType().equals("TRIP");
        if (OfflineOperationType.supports(operation.operationType()) && !aggregateMatches) {
            return OfflineHandlerOutcome.rejected("OFFLINE_SYNC_PAYLOAD_INVALID",
                    "Operation and aggregate types do not match");
        }
        return null;
    }

    private OfflineSyncResult directResult(OperationCommand operation, OfflineHandlerOutcome outcome) {
        return new OfflineSyncResult(operation.operationId(), outcome.status(), now(), operation.aggregateId(),
                null, outcome.errorCode(), outcome.message());
    }

    private OfflineSyncResult retryable(OperationCommand operation) {
        return new OfflineSyncResult(operation.operationId(), OfflineSyncResultStatus.RETRYABLE_ERROR, now(),
                operation.aggregateId(), null, "OFFLINE_SYNC_RETRYABLE",
                "Operation could not be processed and may be retried");
    }

    private OffsetDateTime now() {
        return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
