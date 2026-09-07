package com.transportlogistics.app.fleet.payroll.adapters.outbound.persistence;

import com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "driver_payroll_worker_mapping_command")
@Getter
@Setter
@NoArgsConstructor
class DriverPayrollMappingCommandEntity extends TenantScopedEntity {
    @Id private UUID id;
    @Column(name = "driver_id") private UUID driverId;
    @Column(name = "idempotency_key") private String idempotencyKey;
    @Column(name = "request_hash") private String requestHash;
    @Column(name = "result_mapping_id") private UUID resultMappingId;
    @Column(name = "result_version") private long resultVersion;
    @Column(name = "result_external_system_alias") private String resultExternalSystemAlias;
    @Column(name = "result_external_worker_reference") private String resultExternalWorkerReference;
    @Column(name = "result_active") private boolean resultActive;
    @Column(name = "result_updated_by") private UUID resultUpdatedBy;
    @Column(name = "result_created_at") private OffsetDateTime resultCreatedAt;
    @Column(name = "result_updated_at") private OffsetDateTime resultUpdatedAt;
    @Column(name = "actor_id") private UUID actorId;
    @Column(name = "created_at") private OffsetDateTime createdAt;
}
