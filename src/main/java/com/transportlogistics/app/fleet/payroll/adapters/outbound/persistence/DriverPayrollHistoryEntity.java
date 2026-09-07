package com.transportlogistics.app.fleet.payroll.adapters.outbound.persistence;
import com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity;import jakarta.persistence.*;import lombok.*;import java.time.*;import java.util.*;
@Entity @Table(name="driver_payroll_input_history") @Getter @Setter @NoArgsConstructor
class DriverPayrollHistoryEntity extends TenantScopedEntity {@Id UUID id;@Column(name="batch_id")UUID batchId;String action;@Column(name="from_state")String fromState;@Column(name="to_state")String toState;@Column(name="actor_id")UUID actorId;String detail;@Column(name="created_at")OffsetDateTime createdAt;}
