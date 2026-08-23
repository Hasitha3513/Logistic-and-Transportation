package com.transportlogistics.app.offlinesync.application.ports.out;

import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncOperation;

import java.util.Optional;
import java.util.UUID;

public interface OfflineSyncOperationRepository {
    Optional<OfflineSyncOperation> findByOperationId(UUID operationId);

    OfflineSyncOperation claim(OfflineSyncOperation operation);

    OfflineSyncOperation save(OfflineSyncOperation operation);
}
