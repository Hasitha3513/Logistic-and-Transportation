package com.transportlogistics.app.freight.exception.adapters.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cargo_exception_history")
public class CargoExceptionHistoryEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exception_id", nullable = false)
    private CargoExceptionEntity exceptionEntity;

    @Column(name = "action", nullable = false, length = 60)
    private String action;

    @Column(name = "actor", nullable = false, length = 128)
    private String actor;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "reason", length = 2000)
    private String reason;

    @Column(name = "details", length = 2000)
    private String details;

    public CargoExceptionHistoryEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public CargoExceptionEntity getExceptionEntity() { return exceptionEntity; }
    public void setExceptionEntity(CargoExceptionEntity exceptionEntity) {
        this.exceptionEntity = exceptionEntity;
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }

    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(OffsetDateTime occurredAt) { this.occurredAt = occurredAt; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
