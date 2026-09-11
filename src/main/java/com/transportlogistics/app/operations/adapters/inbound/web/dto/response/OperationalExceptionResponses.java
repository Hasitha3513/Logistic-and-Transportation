package com.transportlogistics.app.operations.adapters.inbound.web.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class OperationalExceptionResponses {
    private OperationalExceptionResponses() {}

    public record Page<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}

    public record Case(UUID id, String caseReference, String sourceModule, String sourceType, UUID sourceId,
                       OffsetDateTime occurredAt, String summaryCode, String category, String severity,
                       String status, String slaStatus, OffsetDateTime responseDueAt,
                       OffsetDateTime resolutionDueAt, OffsetDateTime nextEscalationAt,
                       OffsetDateTime acknowledgedAt, OffsetDateTime resolvedAt, OffsetDateTime closedAt,
                       String assignmentType, UUID assignedUserId, String assignedRoleCode,
                       String escalationLevel, String resolutionNote, String resolutionResultReference,
                       long version, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}

    public record CorrectiveAction(UUID id, String type, String description, String ownerType,
                                   UUID ownerUserId, String ownerRoleCode, OffsetDateTime dueAt,
                                   String status, OffsetDateTime completedAt, String evidenceReference,
                                   long version, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}

    public record Rca(UUID id, String causeCategory, String rootCauseCode, String summary,
                      String contributingFactors, UUID authorId, UUID approverId,
                      OffsetDateTime approvedAt, long version) {}

    public record Detail(Case exceptionCase, List<CorrectiveAction> correctiveActions, Rca rca) {}

    public record History(UUID id, String action, String beforeValue, String afterValue, String reason,
                          UUID actorId, String actorUsername, String correlationId,
                          long resultingVersion, OffsetDateTime occurredAt) {}
}
