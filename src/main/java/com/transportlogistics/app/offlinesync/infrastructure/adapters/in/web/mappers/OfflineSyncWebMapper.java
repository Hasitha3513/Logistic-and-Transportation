package com.transportlogistics.app.offlinesync.infrastructure.adapters.in.web.mappers;

import com.transportlogistics.app.offlinesync.application.ports.in.OfflineSyncUseCase;
import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncResult;
import com.transportlogistics.app.offlinesync.infrastructure.adapters.in.web.dto.request.OfflineSyncOperationRequest;
import com.transportlogistics.app.offlinesync.infrastructure.adapters.in.web.dto.response.OfflineSyncBatchResponse;
import com.transportlogistics.app.offlinesync.infrastructure.adapters.in.web.dto.response.OfflineSyncOperationResponse;
import org.springframework.stereotype.Component;

@Component
public class OfflineSyncWebMapper {
    public OfflineSyncUseCase.OperationCommand toCommand(OfflineSyncOperationRequest request) {
        return new OfflineSyncUseCase.OperationCommand(request.operationId(), request.operationType(),
                request.operationVersion(), request.aggregateType(), request.aggregateId(), request.payload(),
                request.clientCreatedAt(), request.clientUpdatedAt(), request.clientInstanceId(),
                request.idempotencyKey(), request.baseVersion());
    }

    public OfflineSyncBatchResponse toResponse(OfflineSyncUseCase.BatchResult result) {
        return new OfflineSyncBatchResponse(result.serverTimestamp(),
                result.results().stream().map(this::toResponse).toList());
    }

    private OfflineSyncOperationResponse toResponse(OfflineSyncResult result) {
        return new OfflineSyncOperationResponse(result.operationId(), result.status(), result.serverTimestamp(),
                result.aggregateId(), result.currentVersion(), result.errorCode(), result.message());
    }
}
