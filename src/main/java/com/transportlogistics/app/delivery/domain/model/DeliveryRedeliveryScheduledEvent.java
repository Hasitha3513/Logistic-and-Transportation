package com.transportlogistics.app.delivery.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Domain event emitted when a delivery order is successfully scheduled for re-delivery.
 */
public record DeliveryRedeliveryScheduledEvent(
        UUID tenantId,
        UUID deliveryId,
        UUID scheduleId,
        UUID failedAttemptId,
        OffsetDateTime scheduledStartTime,
        OffsetDateTime scheduledEndTime,
        RedeliverySchedulingMethod schedulingMethod,
        OffsetDateTime timestamp
) {
}
