package com.transportlogistics.app.freight.exception.domain;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root for a Cargo Exception (US-30).
 *
 * <p>Owns the common auditable lifecycle for the six source-defined exception types:
 * DAMAGE, PARTIAL_SHIPMENT, WEIGHT_DISCREPANCY, HAZARDOUS_MATERIAL,
 * UNMANIFESTED_CARGO, and SEAL_TAMPERING.
 *
 * <p>Pure domain class — no Spring, JPA, or infrastructure imports.
 */
public final class CargoException {

    // ── Identity ──────────────────────────────────────────────────────────────
    private final UUID id;
    private final String exceptionNumber;

    // ── Classification ────────────────────────────────────────────────────────
    private final ExceptionType exceptionType;
    private final ExceptionStatus status;
    private final ExceptionSeverity severity;

    // ── Cross-module logical references (stored as UUIDs — no physical FKs) ──
    private final UUID freightOrderId;
    private final UUID manifestId;
    private final UUID manifestItemId;

    // ── Description & Operational fields ─────────────────────────────────────
    private final String description;
    private final String impact;
    private final String restriction;
    private final String correctiveAction;
    private final String resolution;

    // ── Resolution Audit ─────────────────────────────────────────────────────
    private final OffsetDateTime resolvedAt;
    private final String resolvedBy;

    // ── Immutable history ─────────────────────────────────────────────────────
    private final List<CargoExceptionHistoryEntry> history;

    // ── Standard audit ───────────────────────────────────────────────────────
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
    private final String createdBy;
    private final String updatedBy;
    private final long version;

    public CargoException(UUID id,
                          String exceptionNumber,
                          ExceptionType exceptionType,
                          ExceptionStatus status,
                          ExceptionSeverity severity,
                          UUID freightOrderId,
                          UUID manifestId,
                          UUID manifestItemId,
                          String description,
                          String impact,
                          String restriction,
                          String correctiveAction,
                          String resolution,
                          OffsetDateTime resolvedAt,
                          String resolvedBy,
                          List<CargoExceptionHistoryEntry> history,
                          OffsetDateTime createdAt,
                          OffsetDateTime updatedAt,
                          String createdBy,
                          String updatedBy,
                          long version) {
        this.id              = Objects.requireNonNull(id, "Exception ID is required");
        if (exceptionNumber == null || exceptionNumber.isBlank()) {
            throw new IllegalArgumentException("Exception number is required");
        }
        this.exceptionNumber  = exceptionNumber;
        this.exceptionType    = Objects.requireNonNull(exceptionType, "Exception type is required");
        this.status           = status != null ? status : ExceptionStatus.OPEN;
        this.severity         = severity != null ? severity : ExceptionSeverity.MEDIUM;
        this.freightOrderId   = Objects.requireNonNull(freightOrderId, "Freight order ID is required");
        this.manifestId       = manifestId;
        this.manifestItemId   = manifestItemId;
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description is required");
        }
        this.description      = description;
        this.impact           = impact;
        this.restriction      = restriction;
        this.correctiveAction = correctiveAction;
        this.resolution       = resolution;
        this.resolvedAt       = resolvedAt;
        this.resolvedBy       = resolvedBy;
        this.history          = history == null ? List.of() : List.copyOf(history);
        this.createdAt        = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt        = Objects.requireNonNull(updatedAt, "updatedAt is required");
        this.createdBy        = Objects.requireNonNull(createdBy, "createdBy is required");
        this.updatedBy        = Objects.requireNonNull(updatedBy, "updatedBy is required");
        if (version < 0) {
            throw new IllegalArgumentException("Version must be non-negative");
        }
        this.version = version;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public UUID getId()                    { return id; }
    public String getExceptionNumber()     { return exceptionNumber; }
    public ExceptionType getExceptionType(){ return exceptionType; }
    public ExceptionStatus getStatus()     { return status; }
    public ExceptionSeverity getSeverity() { return severity; }
    public UUID getFreightOrderId()        { return freightOrderId; }
    public UUID getManifestId()            { return manifestId; }
    public UUID getManifestItemId()        { return manifestItemId; }
    public String getDescription()         { return description; }
    public String getImpact()              { return impact; }
    public String getRestriction()         { return restriction; }
    public String getCorrectiveAction()    { return correctiveAction; }
    public String getResolution()          { return resolution; }
    public OffsetDateTime getResolvedAt()  { return resolvedAt; }
    public String getResolvedBy()          { return resolvedBy; }
    public List<CargoExceptionHistoryEntry> getHistory() { return history; }
    public OffsetDateTime getCreatedAt()   { return createdAt; }
    public OffsetDateTime getUpdatedAt()   { return updatedAt; }
    public String getCreatedBy()           { return createdBy; }
    public String getUpdatedBy()           { return updatedBy; }
    public long getVersion()               { return version; }

    // ── Business helper ───────────────────────────────────────────────────────

    public boolean isOpen()       { return status == ExceptionStatus.OPEN; }
    public boolean isHeld()       { return status == ExceptionStatus.HELD; }
    public boolean isEscalated()  { return status == ExceptionStatus.ESCALATED; }
    public boolean isClosed()     { return status == ExceptionStatus.RESOLVED || status == ExceptionStatus.REJECTED; }

    // ── Workflow Transitions ──────────────────────────────────────────────────

    /**
     * Apply an operational hold.
     * Allowed from: OPEN, ESCALATED
     */
    public CargoException hold(String restriction, String reason, String actor, OffsetDateTime now) {
        if (status != ExceptionStatus.OPEN && status != ExceptionStatus.ESCALATED) {
            throw new ConflictException("CARGO_EXCEPTION_INVALID_STATE",
                    "Cannot hold an exception in status " + status);
        }
        return rebuildWith(
                ExceptionStatus.HELD,
                restriction != null ? restriction : this.restriction,
                this.resolution, null, null,
                appendHistory(new CargoExceptionHistoryEntry(
                        UUID.randomUUID(), id, "HOLD_APPLIED", actor, now, reason, restriction)),
                actor, now
        );
    }

    /**
     * Escalate an OPEN or HELD exception.
     * Allowed from: OPEN, HELD
     */
    public CargoException escalate(String reason, String actor, OffsetDateTime now) {
        if (status != ExceptionStatus.OPEN && status != ExceptionStatus.HELD) {
            throw new ConflictException("CARGO_EXCEPTION_INVALID_STATE",
                    "Cannot escalate an exception in status " + status);
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("ESCALATION_REASON_REQUIRED", "Escalation reason is required");
        }
        return rebuildWith(
                ExceptionStatus.ESCALATED,
                this.restriction, this.resolution, null, null,
                appendHistory(new CargoExceptionHistoryEntry(
                        UUID.randomUUID(), id, "ESCALATED", actor, now, reason, null)),
                actor, now
        );
    }

    /**
     * Release a HELD exception back to OPEN.
     * Allowed from: HELD
     */
    public CargoException release(String reason, String actor, OffsetDateTime now) {
        if (status != ExceptionStatus.HELD) {
            throw new ConflictException("CARGO_EXCEPTION_INVALID_STATE",
                    "Cannot release an exception that is not HELD (status: " + status + ")");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("RELEASE_REASON_REQUIRED", "Release reason is required");
        }
        return rebuildWith(
                ExceptionStatus.OPEN,
                null,   // restriction lifted
                this.resolution, null, null,
                appendHistory(new CargoExceptionHistoryEntry(
                        UUID.randomUUID(), id, "RELEASED", actor, now, reason, null)),
                actor, now
        );
    }

    /**
     * Reject an exception (final closed state — no insurance or correction path).
     * Allowed from: OPEN, HELD, ESCALATED
     */
    public CargoException reject(String reason, String actor, OffsetDateTime now) {
        if (isClosed()) {
            throw new ConflictException("CARGO_EXCEPTION_INVALID_STATE",
                    "Cannot reject an already-closed exception (status: " + status + ")");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("REJECTION_REASON_REQUIRED", "Rejection reason is required");
        }
        return rebuildWith(
                ExceptionStatus.REJECTED,
                this.restriction, reason, now, actor,
                appendHistory(new CargoExceptionHistoryEntry(
                        UUID.randomUUID(), id, "REJECTED", actor, now, reason, null)),
                actor, now
        );
    }

    /**
     * Resolve an exception with a recorded corrective outcome.
     * Allowed from: OPEN, HELD, ESCALATED
     */
    public CargoException resolve(String resolution, String correctiveAction, String reason,
                                  String actor, OffsetDateTime now) {
        if (isClosed()) {
            throw new ConflictException("CARGO_EXCEPTION_INVALID_STATE",
                    "Cannot resolve an already-closed exception (status: " + status + ")");
        }
        if (resolution == null || resolution.isBlank()) {
            throw new BusinessRuleException("RESOLUTION_REQUIRED", "Resolution description is required");
        }
        return rebuildWith(
                ExceptionStatus.RESOLVED,
                this.restriction, resolution, now, actor,
                appendHistory(new CargoExceptionHistoryEntry(
                        UUID.randomUUID(), id, "RESOLVED", actor, now, reason, resolution)),
                actor, now
        );
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<CargoExceptionHistoryEntry> appendHistory(CargoExceptionHistoryEntry entry) {
        List<CargoExceptionHistoryEntry> updated = new ArrayList<>(this.history);
        updated.add(entry);
        return Collections.unmodifiableList(updated);
    }

    private CargoException rebuildWith(ExceptionStatus newStatus,
                                       String newRestriction,
                                       String newResolution,
                                       OffsetDateTime newResolvedAt,
                                       String newResolvedBy,
                                       List<CargoExceptionHistoryEntry> newHistory,
                                       String actor,
                                       OffsetDateTime now) {
        return new CargoException(
                this.id,
                this.exceptionNumber,
                this.exceptionType,
                newStatus,
                this.severity,
                this.freightOrderId,
                this.manifestId,
                this.manifestItemId,
                this.description,
                this.impact,
                newRestriction,
                this.correctiveAction,
                newResolution,
                newResolvedAt != null ? newResolvedAt : this.resolvedAt,
                newResolvedBy != null ? newResolvedBy : this.resolvedBy,
                newHistory,
                this.createdAt,
                now,
                this.createdBy,
                actor,
                this.version
        );
    }
}
