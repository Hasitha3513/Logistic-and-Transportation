package com.transportlogistics.app.operations.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public final class RootCauseAnalysis {
    private final UUID id;
    private final UUID tenantId;
    private final UUID caseId;
    private final CauseCategory causeCategory;
    private final String rootCauseCode;
    private final String summary;
    private final String contributingFactors;
    private final UUID authorId;
    private UUID approverId;
    private OffsetDateTime approvedAt;
    private long version;
    private final OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @SuppressWarnings("java:S107")
    public RootCauseAnalysis(UUID id, UUID tenantId, UUID caseId, CauseCategory causeCategory,
                             String rootCauseCode, String summary, String contributingFactors,
                             UUID authorId, UUID approverId, OffsetDateTime approvedAt, long version,
                             OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.tenantId = Objects.requireNonNull(tenantId);
        this.caseId = Objects.requireNonNull(caseId);
        this.causeCategory = Objects.requireNonNull(causeCategory);
        this.rootCauseCode = text(rootCauseCode, 80, "root cause code");
        this.summary = text(summary, 2000, "RCA summary");
        this.contributingFactors = optional(contributingFactors, 2000);
        this.authorId = Objects.requireNonNull(authorId);
        this.approverId = approverId;
        this.approvedAt = approvedAt;
        this.version = version;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        if ((approverId == null) != (approvedAt == null) || authorId.equals(approverId)) {
            throw new BusinessRuleException("OPERATIONAL_EXCEPTION_RCA_APPROVAL_REQUIRED", "Invalid RCA approval");
        }
    }

    public static RootCauseAnalysis create(UUID id, UUID tenantId, UUID caseId, CauseCategory category,
                                           String code, String summary, String factors, UUID author,
                                           OffsetDateTime now) {
        return new RootCauseAnalysis(id, tenantId, caseId, category, code, summary, factors, author,
            null, null, 0, now, now);
    }

    public void approve(UUID actorId, OffsetDateTime now) {
        if (approvedAt != null || authorId.equals(actorId)) {
            throw new BusinessRuleException("OPERATIONAL_EXCEPTION_RCA_APPROVAL_REQUIRED",
                "RCA approval requires a different actor and may occur only once");
        }
        approverId = actorId;
        approvedAt = now;
        updatedAt = now;
        version++;
    }

    public boolean approved() { return approvedAt != null; }

    private static String text(String value, int max, String label) {
        if (value == null || value.isBlank() || value.trim().length() > max) {
            throw new BusinessRuleException("OPERATIONAL_EXCEPTION_RCA_REQUIRED", "Invalid " + label);
        }
        return value.trim();
    }

    private static String optional(String value, int max) {
        if (value == null || value.isBlank()) return null;
        if (value.trim().length() > max) throw new BusinessRuleException("OPERATIONAL_EXCEPTION_RCA_REQUIRED", "Contributing factors are too long");
        return value.trim();
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public UUID caseId() { return caseId; }
    public CauseCategory causeCategory() { return causeCategory; }
    public String rootCauseCode() { return rootCauseCode; }
    public String summary() { return summary; }
    public String contributingFactors() { return contributingFactors; }
    public UUID authorId() { return authorId; }
    public UUID approverId() { return approverId; }
    public OffsetDateTime approvedAt() { return approvedAt; }
    public long version() { return version; }
    public OffsetDateTime createdAt() { return createdAt; }
    public OffsetDateTime updatedAt() { return updatedAt; }

    public enum CauseCategory { PEOPLE, PROCESS, EQUIPMENT, EXTERNAL, SYSTEM_DATA, ENVIRONMENT, UNKNOWN }
}
