package com.transportlogistics.app.operations.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OperationalExceptionHistory(UUID id, UUID tenantId, UUID caseId, String action,
                                          String beforeValue, String afterValue, String reason,
                                          UUID actorId, String actorUsername, String correlationId,
                                          long resultingVersion, OffsetDateTime occurredAt) {}
