package com.transportlogistics.app.operations.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public final class CorrectiveAction {
    private final UUID id;
    private final UUID tenantId;
    private final UUID caseId;
    private final Type type;
    private final String description;
    private final OperationalExceptionCase.AssignmentType ownerType;
    private final UUID ownerUserId;
    private final String ownerRoleCode;
    private final OffsetDateTime dueAt;
    private Status status;
    private OffsetDateTime completedAt;
    private final String evidenceReference;
    private String cancellationReason;
    private long version;
    private final OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @SuppressWarnings("java:S107")
    public CorrectiveAction(UUID id, UUID tenantId, UUID caseId, Type type, String description,
                            OperationalExceptionCase.AssignmentType ownerType, UUID ownerUserId,
                            String ownerRoleCode, OffsetDateTime dueAt, Status status,
                            OffsetDateTime completedAt, String evidenceReference, String cancellationReason,
                            long version, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.tenantId = Objects.requireNonNull(tenantId);
        this.caseId = Objects.requireNonNull(caseId);
        this.type = Objects.requireNonNull(type);
        this.description = text(description, 2000, "description");
        this.ownerType = Objects.requireNonNull(ownerType);
        this.ownerUserId = ownerUserId;
        this.ownerRoleCode = optional(ownerRoleCode, 80);
        this.dueAt = dueAt;
        this.status = Objects.requireNonNull(status);
        this.completedAt = completedAt;
        this.evidenceReference = optional(evidenceReference, 160);
        this.cancellationReason = optional(cancellationReason, 2000);
        this.version = version;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        boolean validOwner = ownerType == OperationalExceptionCase.AssignmentType.USER
            ? ownerUserId != null && ownerRoleCode == null : ownerUserId == null && ownerRoleCode != null;
        if (!validOwner) throw new BusinessRuleException("OPERATIONAL_EXCEPTION_ASSIGNMENT_INVALID", "Invalid corrective action owner");
    }

    public static CorrectiveAction open(UUID id, UUID tenantId, UUID caseId, Type type, String description,
                                        OperationalExceptionCase.AssignmentType ownerType, UUID ownerUserId,
                                        String ownerRoleCode, OffsetDateTime dueAt, String evidenceReference,
                                        OffsetDateTime now) {
        return new CorrectiveAction(id, tenantId, caseId, type, description, ownerType, ownerUserId,
            ownerRoleCode, dueAt, Status.OPEN, null, evidenceReference, null, 0, now, now);
    }

    public void start(OffsetDateTime now) {
        if (status != Status.OPEN) invalid();
        status = Status.IN_PROGRESS;
        version++;
        updatedAt = now;
    }

    public void complete(OffsetDateTime now) {
        if (status != Status.OPEN && status != Status.IN_PROGRESS) invalid();
        status = Status.COMPLETED;
        completedAt = now;
        version++;
        updatedAt = now;
    }

    public void cancel(String reason, OffsetDateTime now) {
        if (status != Status.OPEN && status != Status.IN_PROGRESS) invalid();
        cancellationReason = text(reason, 2000, "cancellation reason");
        status = Status.CANCELLED;
        version++;
        updatedAt = now;
    }

    private static void invalid() {
        throw new BusinessRuleException("OPERATIONAL_EXCEPTION_INVALID_TRANSITION", "Corrective action transition is not permitted");
    }

    private static String text(String value, int max, String label) {
        if (value == null || value.isBlank() || value.trim().length() > max) {
            throw new BusinessRuleException("OPERATIONAL_EXCEPTION_CLOSE_NOT_ALLOWED", "Invalid " + label);
        }
        return value.trim();
    }

    private static String optional(String value, int max) {
        if (value == null || value.isBlank()) return null;
        if (value.trim().length() > max) throw new BusinessRuleException("OPERATIONAL_EXCEPTION_CLOSE_NOT_ALLOWED", "Value is too long");
        return value.trim();
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public UUID caseId() { return caseId; }
    public Type type() { return type; }
    public String description() { return description; }
    public OperationalExceptionCase.AssignmentType ownerType() { return ownerType; }
    public UUID ownerUserId() { return ownerUserId; }
    public String ownerRoleCode() { return ownerRoleCode; }
    public OffsetDateTime dueAt() { return dueAt; }
    public Status status() { return status; }
    public OffsetDateTime completedAt() { return completedAt; }
    public String evidenceReference() { return evidenceReference; }
    public String cancellationReason() { return cancellationReason; }
    public long version() { return version; }
    public OffsetDateTime createdAt() { return createdAt; }
    public OffsetDateTime updatedAt() { return updatedAt; }

    public enum Type { CORRECTIVE, PREVENTIVE }
    public enum Status { OPEN, IN_PROGRESS, COMPLETED, CANCELLED }
}
