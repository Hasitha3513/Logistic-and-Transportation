package com.transportlogistics.app.delivery.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Pure domain entity representing a scheduled re-delivery attempt.
 */
public class DeliveryRedeliverySchedule {

    private final UUID id;
    private final UUID tenantId;
    private final DeliveryId deliveryOrderId;
    private final UUID deliveryAttemptId;
    private final RedeliverySchedulingMethod schedulingMethod;
    private final OffsetDateTime preferredStartTime;
    private final OffsetDateTime preferredEndTime;
    private final String customerPreferenceNotes;
    private final OffsetDateTime scheduledStartTime;
    private final OffsetDateTime scheduledEndTime;
    private RedeliveryScheduleStatus status;
    private final String scheduledBy;
    private final OffsetDateTime scheduledAt;
    private OffsetDateTime supersededAt;
    private String supersededBy;
    private String supersedeReason;
    private final OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public DeliveryRedeliverySchedule(
            UUID id,
            UUID tenantId,
            DeliveryId deliveryOrderId,
            UUID deliveryAttemptId,
            RedeliverySchedulingMethod schedulingMethod,
            OffsetDateTime preferredStartTime,
            OffsetDateTime preferredEndTime,
            String customerPreferenceNotes,
            OffsetDateTime scheduledStartTime,
            OffsetDateTime scheduledEndTime,
            RedeliveryScheduleStatus status,
            String scheduledBy,
            OffsetDateTime scheduledAt,
            OffsetDateTime supersededAt,
            String supersededBy,
            String supersedeReason,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "Schedule ID must not be null");
        this.tenantId = Objects.requireNonNull(tenantId, "Tenant ID must not be null");
        this.deliveryOrderId = Objects.requireNonNull(deliveryOrderId, "Delivery Order ID must not be null");
        this.deliveryAttemptId = Objects.requireNonNull(deliveryAttemptId, "Delivery Attempt ID must not be null");
        this.schedulingMethod = Objects.requireNonNull(schedulingMethod, "Scheduling method must not be null");
        this.preferredStartTime = preferredStartTime;
        this.preferredEndTime = preferredEndTime;
        this.customerPreferenceNotes = validateCustomerPreferenceNotes(customerPreferenceNotes);
        this.scheduledStartTime = Objects.requireNonNull(scheduledStartTime, "Scheduled start time must not be null");
        this.scheduledEndTime = Objects.requireNonNull(scheduledEndTime, "Scheduled end time must not be null");
        this.status = Objects.requireNonNull(status, "Status must not be null");
        this.scheduledBy = Objects.requireNonNull(scheduledBy, "Scheduled by must not be null");
        this.scheduledAt = Objects.requireNonNull(scheduledAt, "Scheduled at must not be null");
        this.supersededAt = supersededAt;
        this.supersededBy = supersededBy;
        this.supersedeReason = supersedeReason;
        this.createdAt = Objects.requireNonNull(createdAt, "Created at must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated at must not be null");

        validateDeliveryWindow(scheduledStartTime, scheduledEndTime);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public static DeliveryRedeliverySchedule createConfirmed(
            UUID id,
            UUID tenantId,
            DeliveryId deliveryOrderId,
            UUID deliveryAttemptId,
            RedeliverySchedulingMethod schedulingMethod,
            OffsetDateTime preferredStartTime,
            OffsetDateTime preferredEndTime,
            String customerPreferenceNotes,
            OffsetDateTime scheduledStartTime,
            OffsetDateTime scheduledEndTime,
            String scheduledBy,
            OffsetDateTime now
    ) {
        validateWindowFutureAndHorizon(scheduledStartTime, scheduledEndTime, now);
        return new DeliveryRedeliverySchedule(
                id,
                tenantId,
                deliveryOrderId,
                deliveryAttemptId,
                schedulingMethod,
                preferredStartTime,
                preferredEndTime,
                customerPreferenceNotes,
                scheduledStartTime,
                scheduledEndTime,
                RedeliveryScheduleStatus.CONFIRMED,
                scheduledBy,
                now,
                null,
                null,
                null,
                now,
                now
        );
    }

    public void supersede(String actor, String reason, OffsetDateTime now) {
        if (this.status != RedeliveryScheduleStatus.CONFIRMED) {
            throw new BusinessRuleException("INVALID_SCHEDULE_STATUS", "Only CONFIRMED schedules can be superseded");
        }
        this.status = RedeliveryScheduleStatus.SUPERSEDED;
        this.supersededBy = actor;
        this.supersedeReason = reason;
        this.supersededAt = now;
        this.updatedAt = now;
    }

    private static String validateCustomerPreferenceNotes(String notes) {
        if (notes != null && notes.trim().length() > 500) {
            throw new BusinessRuleException("INVALID_PREFERENCE_NOTES", "Customer preference notes must not exceed 500 characters");
        }
        return notes != null && !notes.trim().isEmpty() ? notes.trim() : null;
    }

    public static void validateDeliveryWindow(OffsetDateTime start, OffsetDateTime end) {
        if (!start.isBefore(end)) {
            throw new BusinessRuleException("INVALID_DELIVERY_WINDOW", "Scheduled start time must be strictly before end time");
        }
        Duration duration = Duration.between(start, end);
        if (duration.toMinutes() < 30) {
            throw new BusinessRuleException("INVALID_WINDOW_DURATION", "Scheduled delivery window must be at least 30 minutes in duration");
        }
        if (duration.toHours() > 24) {
            throw new BusinessRuleException("INVALID_WINDOW_DURATION", "Scheduled delivery window must not exceed 24 hours in duration");
        }
    }

    public static void validateWindowFutureAndHorizon(OffsetDateTime start, OffsetDateTime end, OffsetDateTime now) {
        validateDeliveryWindow(start, end);
        if (start.isBefore(now)) {
            throw new BusinessRuleException("DELIVERY_WINDOW_IN_PAST", "Scheduled delivery window start time cannot be in the past");
        }
        if (start.isAfter(now.plusDays(30))) {
            throw new BusinessRuleException("HORIZON_EXCEEDED", "Scheduled delivery window cannot exceed the 30-day scheduling horizon");
        }
    }

    public UUID id() {
        return id;
    }

    public UUID tenantId() {
        return tenantId;
    }

    public DeliveryId deliveryOrderId() {
        return deliveryOrderId;
    }

    public UUID deliveryAttemptId() {
        return deliveryAttemptId;
    }

    public RedeliverySchedulingMethod schedulingMethod() {
        return schedulingMethod;
    }

    public OffsetDateTime preferredStartTime() {
        return preferredStartTime;
    }

    public OffsetDateTime preferredEndTime() {
        return preferredEndTime;
    }

    public String customerPreferenceNotes() {
        return customerPreferenceNotes;
    }

    public OffsetDateTime scheduledStartTime() {
        return scheduledStartTime;
    }

    public OffsetDateTime scheduledEndTime() {
        return scheduledEndTime;
    }

    public RedeliveryScheduleStatus status() {
        return status;
    }

    public String scheduledBy() {
        return scheduledBy;
    }

    public OffsetDateTime scheduledAt() {
        return scheduledAt;
    }

    public OffsetDateTime supersededAt() {
        return supersededAt;
    }

    public String supersededBy() {
        return supersededBy;
    }

    public String supersedeReason() {
        return supersedeReason;
    }

    public OffsetDateTime createdAt() {
        return createdAt;
    }

    public OffsetDateTime updatedAt() {
        return updatedAt;
    }
}
