package com.transportlogistics.app.offlinesync.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncOperation;
import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncResultStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "offline_sync_operation")
public class OfflineSyncOperationEntity {
    @Id
    @Column(name = "operation_id")
    private UUID operationId;
    @Column(name = "operation_type", nullable = false, length = 64)
    private String operationType;
    @Column(name = "operation_version", nullable = false)
    private int operationVersion;
    @Column(name = "actor_id", nullable = false)
    private UUID actorId;
    @Column(name = "client_instance_id", nullable = false)
    private UUID clientInstanceId;
    @Column(name = "aggregate_type", nullable = false, length = 32)
    private String aggregateType;
    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;
    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;
    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, length = 32)
    private OfflineSyncResultStatus resultStatus;
    @Column(name = "result_code", length = 64)
    private String resultCode;
    @Column(name = "result_version")
    private Long resultVersion;
    @Column(name = "processed_at", nullable = false)
    private OffsetDateTime processedAt;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected OfflineSyncOperationEntity() {
    }

    static OfflineSyncOperationEntity fromDomain(OfflineSyncOperation value) {
        OfflineSyncOperationEntity entity = new OfflineSyncOperationEntity();
        entity.operationId = value.operationId();
        entity.operationType = value.operationType();
        entity.operationVersion = value.operationVersion();
        entity.actorId = value.actorId();
        entity.clientInstanceId = value.clientInstanceId();
        entity.aggregateType = value.aggregateType();
        entity.aggregateId = value.aggregateId();
        entity.requestHash = value.requestHash();
        entity.resultStatus = value.resultStatus();
        entity.resultCode = value.resultCode();
        entity.resultVersion = value.resultVersion();
        entity.processedAt = value.processedAt();
        entity.createdAt = value.createdAt();
        return entity;
    }

    OfflineSyncOperation toDomain() {
        return new OfflineSyncOperation(operationId, operationType, operationVersion, actorId, clientInstanceId,
                aggregateType, aggregateId, requestHash, resultStatus, resultCode, resultVersion,
                processedAt, createdAt);
    }
}
