package com.transportlogistics.app.fleet.payroll.adapters.outbound.persistence;
import com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity;import jakarta.persistence.*;import lombok.*;import java.time.*;import java.util.*;
@Entity @Table(name="driver_payroll_worker_mapping") @Getter @Setter @NoArgsConstructor
class DriverPayrollWorkerMappingEntity extends TenantScopedEntity {@Id UUID id;@Column(name="driver_id")UUID driverId;@Column(name="external_system_alias")String externalSystemAlias;@Column(name="external_worker_reference")String externalWorkerReference;boolean active;@Version long version;@Column(name="updated_by")UUID updatedBy;@Column(name="created_at")OffsetDateTime createdAt;@Column(name="updated_at")OffsetDateTime updatedAt;}
