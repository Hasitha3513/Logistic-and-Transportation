package com.transportlogistics.app.fuel.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FuelIssueHistory(UUID id, UUID fuelIssueId, FuelIssueStatus fromStatus, FuelIssueStatus toStatus,
                               String action, UUID actorId, String actor, String comment,
                               OffsetDateTime occurredAt) {
}
