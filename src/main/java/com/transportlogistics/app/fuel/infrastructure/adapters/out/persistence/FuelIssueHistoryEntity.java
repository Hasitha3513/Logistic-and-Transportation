package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fuel.domain.model.FuelIssueStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "fuel_issue_history")
@Getter
@Setter
@NoArgsConstructor
class FuelIssueHistoryEntity {
    @Id UUID id;
    @Column(name = "fuel_issue_id", nullable = false) UUID fuelIssueId;
    @Enumerated(EnumType.STRING) @Column(name = "from_status") FuelIssueStatus fromStatus;
    @Enumerated(EnumType.STRING) @Column(name = "to_status", nullable = false) FuelIssueStatus toStatus;
    @Column(nullable = false) String action;
    @Column(name = "actor_id", nullable = false) UUID actorId;
    @Column(nullable = false) String actor;
    @Column(length = 1000) String comment;
    @Column(name = "occurred_at", nullable = false) OffsetDateTime occurredAt;
}
