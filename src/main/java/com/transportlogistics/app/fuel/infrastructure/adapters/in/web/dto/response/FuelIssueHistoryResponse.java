package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.fuel.domain.model.FuelIssueStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FuelIssueHistoryResponse(UUID id,
                                       UUID fuelIssueId,
                                       FuelIssueStatus fromStatus,
                                       FuelIssueStatus toStatus,
                                       String action,
                                       UUID actorId,
                                       String actor,
                                       String comment,
                                       OffsetDateTime occurredAt) {
}
