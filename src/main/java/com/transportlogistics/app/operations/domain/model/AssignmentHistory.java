package com.transportlogistics.app.operations.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AssignmentHistory(UUID id, UUID tenantId, UUID caseId,
                                OperationalExceptionCase.AssignmentType fromType, UUID fromUserId,
                                String fromRoleCode, OperationalExceptionCase.AssignmentType toType,
                                UUID toUserId, String toRoleCode, UUID actorId, String actorUsername,
                                String reason, OffsetDateTime occurredAt) {}
