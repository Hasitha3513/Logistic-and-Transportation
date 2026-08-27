package com.transportlogistics.app.freight.exception.domain;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable audit-history entry for a CargoException lifecycle action.
 * Records are never mutated after creation.
 */
public final class CargoExceptionHistoryEntry {

    private final UUID id;
    private final UUID exceptionId;
    private final String action;
    private final String actor;
    private final OffsetDateTime occurredAt;
    private final String reason;
    private final String details;

    public CargoExceptionHistoryEntry(UUID id,
                                      UUID exceptionId,
                                      String action,
                                      String actor,
                                      OffsetDateTime occurredAt,
                                      String reason,
                                      String details) {
        this.id          = Objects.requireNonNull(id, "History entry ID is required");
        this.exceptionId = Objects.requireNonNull(exceptionId, "Exception ID is required");
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("Action label is required");
        }
        this.action     = action;
        this.actor      = Objects.requireNonNull(actor, "Actor is required");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt is required");
        this.reason     = reason;
        this.details    = details;
    }

    public UUID getId()             { return id; }
    public UUID getExceptionId()    { return exceptionId; }
    public String getAction()       { return action; }
    public String getActor()        { return actor; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public String getReason()       { return reason; }
    public String getDetails()      { return details; }
}
