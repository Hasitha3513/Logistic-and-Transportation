package com.transportlogistics.app.operations.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public final class OperationalExceptionCase {
    private final UUID id;
    private final UUID tenantId;
    private final String caseReference;
    private final UUID sourceEventId;
    private final SourceModule sourceModule;
    private final String sourceType;
    private final UUID sourceId;
    private final OffsetDateTime occurredAt;
    private final String summaryCode;
    private final String correlationId;
    private Category category;
    private Severity severity;
    private Status status;
    private OffsetDateTime responseDueAt;
    private OffsetDateTime resolutionDueAt;
    private OffsetDateTime nextEscalationAt;
    private OffsetDateTime acknowledgedAt;
    private OffsetDateTime resolvedAt;
    private OffsetDateTime closedAt;
    private AssignmentType assignmentType;
    private UUID assignedUserId;
    private String assignedRoleCode;
    private EscalationLevel escalationLevel;
    private String resolutionNote;
    private String resolutionResultReference;
    private UUID resolvedBy;
    private UUID closedBy;
    private boolean resolutionValidated;
    private long version;
    private final OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @SuppressWarnings("java:S107")
    public OperationalExceptionCase(UUID id, UUID tenantId, String caseReference, UUID sourceEventId,
                                    SourceModule sourceModule, String sourceType, UUID sourceId,
                                    OffsetDateTime occurredAt, String summaryCode, String correlationId,
                                    Category category, Severity severity, Status status,
                                    OffsetDateTime responseDueAt, OffsetDateTime resolutionDueAt,
                                    OffsetDateTime nextEscalationAt, OffsetDateTime acknowledgedAt,
                                    OffsetDateTime resolvedAt, OffsetDateTime closedAt,
                                    AssignmentType assignmentType, UUID assignedUserId, String assignedRoleCode,
                                    EscalationLevel escalationLevel, String resolutionNote,
                                    String resolutionResultReference, UUID resolvedBy, UUID closedBy,
                                    boolean resolutionValidated, long version, OffsetDateTime createdAt,
                                    OffsetDateTime updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.tenantId = Objects.requireNonNull(tenantId);
        this.caseReference = required(caseReference, 16, "case reference");
        this.sourceEventId = Objects.requireNonNull(sourceEventId);
        this.sourceModule = Objects.requireNonNull(sourceModule);
        this.sourceType = required(sourceType, 80, "source type");
        this.sourceId = Objects.requireNonNull(sourceId);
        this.occurredAt = Objects.requireNonNull(occurredAt);
        this.summaryCode = required(summaryCode, 80, "summary code");
        this.correlationId = optional(correlationId, 128, "correlation id");
        this.category = Objects.requireNonNull(category);
        this.severity = Objects.requireNonNull(severity);
        this.status = Objects.requireNonNull(status);
        this.responseDueAt = Objects.requireNonNull(responseDueAt);
        this.resolutionDueAt = Objects.requireNonNull(resolutionDueAt);
        this.nextEscalationAt = nextEscalationAt;
        this.acknowledgedAt = acknowledgedAt;
        this.resolvedAt = resolvedAt;
        this.closedAt = closedAt;
        this.assignmentType = assignmentType;
        this.assignedUserId = assignedUserId;
        this.assignedRoleCode = optional(assignedRoleCode, 80, "role code");
        this.escalationLevel = Objects.requireNonNull(escalationLevel);
        this.resolutionNote = optional(resolutionNote, 2000, "resolution note");
        this.resolutionResultReference = optional(resolutionResultReference, 160, "result reference");
        this.resolvedBy = resolvedBy;
        this.closedBy = closedBy;
        this.resolutionValidated = resolutionValidated;
        this.version = version;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        validateAssignment();
    }

    public static OperationalExceptionCase open(UUID id, UUID tenantId, String reference, UUID sourceEventId,
                                                 SourceModule sourceModule, String sourceType, UUID sourceId,
                                                 OffsetDateTime occurredAt, String summaryCode, String correlationId,
                                                 Category category, Severity severity, String roleCode,
                                                 OffsetDateTime now) {
        return new OperationalExceptionCase(id, tenantId, reference, sourceEventId, sourceModule, sourceType,
            sourceId, occurredAt, summaryCode, correlationId, category, severity, Status.OPEN,
            occurredAt.plus(responseTarget(severity)), occurredAt.plus(resolutionTarget(severity)),
            atRiskAt(occurredAt, severity), null, null, null, AssignmentType.ROLE_QUEUE, null, roleCode,
            severity == Severity.CRITICAL ? EscalationLevel.L1 : EscalationLevel.L0, null, null, null, null,
            false, 0, now, now);
    }

    public void classify(Category newCategory, Severity newSeverity, String reason, OffsetDateTime now) {
        required(reason, 2000, "classification reason");
        category = Objects.requireNonNull(newCategory);
        severity = Objects.requireNonNull(newSeverity);
        responseDueAt = occurredAt.plus(responseTarget(severity));
        resolutionDueAt = occurredAt.plus(resolutionTarget(severity));
        nextEscalationAt = atRiskAt(occurredAt, severity);
        if (severity == Severity.CRITICAL && escalationLevel == EscalationLevel.L0) escalationLevel = EscalationLevel.L1;
        touch(now);
    }

    public void acknowledge(OffsetDateTime now) {
        requireStatus(Status.OPEN);
        status = Status.ACKNOWLEDGED;
        acknowledgedAt = now;
        touch(now);
    }

    public void start(OffsetDateTime now) {
        if (status != Status.OPEN && status != Status.ACKNOWLEDGED) invalidTransition();
        if (acknowledgedAt == null) acknowledgedAt = now;
        status = Status.IN_PROGRESS;
        touch(now);
    }

    public void assign(AssignmentType type, UUID userId, String roleCode, OffsetDateTime now) {
        assignmentType = type;
        assignedUserId = userId;
        assignedRoleCode = optional(roleCode, 80, "role code");
        validateAssignment();
        if (severity == Severity.CRITICAL && type == null) {
            throw new BusinessRuleException("OPERATIONAL_EXCEPTION_ASSIGNMENT_INVALID",
                "Critical operational exceptions cannot be unassigned");
        }
        touch(now);
    }

    public void escalate(OffsetDateTime now) {
        if (status == Status.RESOLVED || status == Status.CLOSED || escalationLevel == EscalationLevel.L3) {
            throw new BusinessRuleException("OPERATIONAL_EXCEPTION_CLOSE_NOT_ALLOWED",
                "Operational exception cannot be escalated further");
        }
        escalationLevel = EscalationLevel.values()[escalationLevel.ordinal() + 1];
        nextEscalationAt = escalationLevel == EscalationLevel.L3 ? null : now.plus(Duration.ofHours(1));
        touch(now);
    }

    public void resolve(String note, String resultReference, UUID actorId, OffsetDateTime now) {
        requireStatus(Status.IN_PROGRESS);
        resolutionNote = required(note, 2000, "resolution note");
        resolutionResultReference = optional(resultReference, 160, "result reference");
        resolvedBy = Objects.requireNonNull(actorId);
        resolvedAt = now;
        status = Status.RESOLVED;
        nextEscalationAt = null;
        resolutionValidated = true;
        touch(now);
    }

    public void close(UUID actorId, boolean rcaReady, OffsetDateTime now) {
        requireStatus(Status.RESOLVED);
        if ((severity == Severity.HIGH || severity == Severity.CRITICAL) && !rcaReady) {
            throw new BusinessRuleException("OPERATIONAL_EXCEPTION_RCA_REQUIRED",
                "Approved root cause analysis is required before closure");
        }
        if (!resolutionValidated) {
            throw new BusinessRuleException("OPERATIONAL_EXCEPTION_CLOSE_NOT_ALLOWED",
                "Resolution must be validated before closure");
        }
        if ((severity == Severity.HIGH || severity == Severity.CRITICAL) && actorId.equals(resolvedBy)) {
            throw new BusinessRuleException("OPERATIONAL_EXCEPTION_CLOSE_NOT_ALLOWED",
                "High and critical cases require a closer different from the resolver");
        }
        closedBy = Objects.requireNonNull(actorId);
        closedAt = now;
        status = Status.CLOSED;
        touch(now);
    }

    public void rejectResolution(String reason, OffsetDateTime now) {
        requireStatus(Status.RESOLVED);
        required(reason, 2000, "rejection reason");
        reopenState(now);
    }

    public void reopen(String reason, OffsetDateTime now) {
        requireStatus(Status.CLOSED);
        required(reason, 2000, "reopen reason");
        reopenState(now);
    }

    private void reopenState(OffsetDateTime now) {
        status = Status.IN_PROGRESS;
        resolvedAt = null;
        closedAt = null;
        resolvedBy = null;
        closedBy = null;
        resolutionValidated = false;
        resolutionNote = null;
        resolutionResultReference = null;
        resolutionDueAt = now.plus(resolutionTarget(severity));
        nextEscalationAt = atRiskAt(now, severity);
        touch(now);
    }

    public SlaStatus slaStatus(OffsetDateTime now) {
        if (resolvedAt != null) return resolvedAt.isAfter(resolutionDueAt) ? SlaStatus.BREACHED : SlaStatus.MET;
        if (!now.isBefore(resolutionDueAt)) return SlaStatus.BREACHED;
        return nextEscalationAt != null && !now.isBefore(nextEscalationAt) ? SlaStatus.AT_RISK : SlaStatus.ON_TRACK;
    }

    private void requireStatus(Status required) {
        if (status != required) invalidTransition();
    }

    private void invalidTransition() {
        throw new BusinessRuleException("OPERATIONAL_EXCEPTION_INVALID_TRANSITION",
            "Transition is not permitted from " + status);
    }

    private void validateAssignment() {
        boolean valid = (assignmentType == null && assignedUserId == null && assignedRoleCode == null)
            || (assignmentType == AssignmentType.USER && assignedUserId != null && assignedRoleCode == null)
            || (assignmentType == AssignmentType.ROLE_QUEUE && assignedUserId == null && assignedRoleCode != null);
        if (!valid) throw new BusinessRuleException("OPERATIONAL_EXCEPTION_ASSIGNMENT_INVALID", "Invalid assignment target");
    }

    private void touch(OffsetDateTime now) {
        updatedAt = Objects.requireNonNull(now);
        version++;
    }

    private static Duration responseTarget(Severity severity) {
        return switch (severity) {
            case LOW -> Duration.ofHours(8);
            case MEDIUM -> Duration.ofHours(4);
            case HIGH -> Duration.ofHours(1);
            case CRITICAL -> Duration.ofMinutes(15);
        };
    }

    private static Duration resolutionTarget(Severity severity) {
        return switch (severity) {
            case LOW -> Duration.ofHours(72);
            case MEDIUM -> Duration.ofHours(24);
            case HIGH -> Duration.ofHours(8);
            case CRITICAL -> Duration.ofHours(2);
        };
    }

    private static OffsetDateTime atRiskAt(OffsetDateTime start, Severity severity) {
        Duration target = resolutionTarget(severity);
        return start.plus(target.multipliedBy(3).dividedBy(4));
    }

    private static String required(String value, int max, String label) {
        if (value == null || value.isBlank()) throw new BusinessRuleException("OPERATIONAL_EXCEPTION_SLA_INVALID", label + " is required");
        String trimmed = value.trim();
        if (trimmed.length() > max) throw new BusinessRuleException("OPERATIONAL_EXCEPTION_SLA_INVALID", label + " is too long");
        return trimmed;
    }

    private static String optional(String value, int max, String label) {
        return value == null || value.isBlank() ? null : required(value, max, label);
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public String caseReference() { return caseReference; }
    public UUID sourceEventId() { return sourceEventId; }
    public SourceModule sourceModule() { return sourceModule; }
    public String sourceType() { return sourceType; }
    public UUID sourceId() { return sourceId; }
    public OffsetDateTime occurredAt() { return occurredAt; }
    public String summaryCode() { return summaryCode; }
    public String correlationId() { return correlationId; }
    public Category category() { return category; }
    public Severity severity() { return severity; }
    public Status status() { return status; }
    public OffsetDateTime responseDueAt() { return responseDueAt; }
    public OffsetDateTime resolutionDueAt() { return resolutionDueAt; }
    public OffsetDateTime nextEscalationAt() { return nextEscalationAt; }
    public OffsetDateTime acknowledgedAt() { return acknowledgedAt; }
    public OffsetDateTime resolvedAt() { return resolvedAt; }
    public OffsetDateTime closedAt() { return closedAt; }
    public AssignmentType assignmentType() { return assignmentType; }
    public UUID assignedUserId() { return assignedUserId; }
    public String assignedRoleCode() { return assignedRoleCode; }
    public EscalationLevel escalationLevel() { return escalationLevel; }
    public String resolutionNote() { return resolutionNote; }
    public String resolutionResultReference() { return resolutionResultReference; }
    public UUID resolvedBy() { return resolvedBy; }
    public UUID closedBy() { return closedBy; }
    public boolean resolutionValidated() { return resolutionValidated; }
    public long version() { return version; }
    public OffsetDateTime createdAt() { return createdAt; }
    public OffsetDateTime updatedAt() { return updatedAt; }

    public enum SourceModule { ROUTING, DELIVERY }
    public enum Category { OPERATIONAL, SAFETY, COMPLIANCE, CUSTOMER, FINANCIAL, TECHNICAL, SECURITY }
    public enum Severity { LOW, MEDIUM, HIGH, CRITICAL }
    public enum Status { OPEN, ACKNOWLEDGED, IN_PROGRESS, RESOLVED, CLOSED }
    public enum SlaStatus { ON_TRACK, AT_RISK, BREACHED, MET }
    public enum AssignmentType { ROLE_QUEUE, USER }
    public enum EscalationLevel { L0, L1, L2, L3 }
}
