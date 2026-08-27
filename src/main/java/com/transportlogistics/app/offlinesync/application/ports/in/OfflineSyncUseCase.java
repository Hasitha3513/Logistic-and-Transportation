package com.transportlogistics.app.offlinesync.application.ports.in;

import com.fasterxml.jackson.databind.JsonNode;
import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncResult;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface OfflineSyncUseCase {
    BatchResult synchronize(BatchCommand command);

    record BatchCommand(String username, Set<String> authorities, List<OperationCommand> operations) {
        public BatchCommand {
            authorities = authorities == null ? Set.of() : Set.copyOf(authorities);
            operations = operations == null ? null : List.copyOf(operations);
        }
    }

    record OperationCommand(
            UUID operationId,
            String operationType,
            int operationVersion,
            String aggregateType,
            UUID aggregateId,
            JsonNode payload,
            OffsetDateTime clientCreatedAt,
            OffsetDateTime clientUpdatedAt,
            UUID clientInstanceId,
            String idempotencyKey,
            Long baseVersion
    ) {
    }

    record BatchResult(OffsetDateTime serverTimestamp, List<OfflineSyncResult> results) {
        public BatchResult {
            results = List.copyOf(results);
        }
    }
}
