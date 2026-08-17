package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fuel.domain.model.FuelIssueStatus;
import jakarta.persistence.*;
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
    @Id
    private UUID id;
    @Column(name = "fuel_issue_id", nullable = false)
    private UUID fuelIssueId;
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private FuelIssueStatus fromStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private FuelIssueStatus toStatus;
    @Column(nullable = false)
    private String action;
    @Column(name = "actor_id", nullable = false)
    private UUID actorId;
    @Column(nullable = false)
    private String actor;
    @Column(length = 1000)
    private String comment;
    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;
}
