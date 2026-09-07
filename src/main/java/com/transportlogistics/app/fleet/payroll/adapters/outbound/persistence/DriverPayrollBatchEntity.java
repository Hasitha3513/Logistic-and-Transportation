package com.transportlogistics.app.fleet.payroll.adapters.outbound.persistence;
import com.transportlogistics.app.fleet.payroll.domain.DriverPayrollInputBatch;
import com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity;
import jakarta.persistence.*;import lombok.*;import java.math.BigDecimal;import java.time.*;import java.util.*;
import org.hibernate.annotations.JdbcTypeCode;
@Entity @Table(name="driver_payroll_input_batch") @Getter @Setter @NoArgsConstructor
class DriverPayrollBatchEntity extends TenantScopedEntity {
 @Id UUID id;@Enumerated(EnumType.STRING) @Column(name="batch_type") DriverPayrollInputBatch.Type type;
 @Column(name="correction_of_batch_id") UUID correctionOfBatchId;@Column(name="period_start") LocalDate periodStart;
 @Column(name="period_end_exclusive") LocalDate periodEndExclusive;@Column(name="cutoff_at") OffsetDateTime cutoffAt;
 @JdbcTypeCode(java.sql.Types.CHAR) @Column(length=3) String currency;@Enumerated(EnumType.STRING) DriverPayrollInputBatch.Lifecycle lifecycle;
 @Column(name="trip_earnings") BigDecimal tripEarnings;BigDecimal allowances;BigDecimal overtime;BigDecimal deductions;
 @Column(name="provisional_net_input") BigDecimal provisionalNetInput;@Column(name="prepared_by") UUID preparedBy;
 @Column(name="approved_by") UUID approvedBy;@Column(name="approved_at") OffsetDateTime approvedAt;
 @Column(name="export_configuration_id") UUID exportConfigurationId;@Column(name="export_event_id") UUID exportEventId;
 @Column(name="validation_hash") String validationHash;@Column(name="idempotency_key") String idempotencyKey;
 @Version long version;@Column(name="created_at") OffsetDateTime createdAt;@Column(name="updated_at") OffsetDateTime updatedAt;
}
