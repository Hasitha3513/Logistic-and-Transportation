package com.transportlogistics.app.freight.loadplanning.domain.event;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Domain event published when a LoadPlan is created.
 */
public record LoadPlanCreated(UUID loadPlanId, OffsetDateTime timestamp) {
}
