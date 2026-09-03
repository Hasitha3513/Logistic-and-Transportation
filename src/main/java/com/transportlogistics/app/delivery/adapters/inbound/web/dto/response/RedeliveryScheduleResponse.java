package com.transportlogistics.app.delivery.adapters.inbound.web.dto.response;

import com.transportlogistics.app.delivery.domain.model.DeliveryRedeliverySchedule;
import com.transportlogistics.app.delivery.domain.model.RedeliveryScheduleStatus;
import com.transportlogistics.app.delivery.domain.model.RedeliverySchedulingMethod;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RedeliveryScheduleResponse(
        UUID id,
        UUID deliveryOrderId,
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
    public static RedeliveryScheduleResponse fromDomain(DeliveryRedeliverySchedule domain) {
        return new RedeliveryScheduleResponse(
                domain.id(),
                domain.deliveryOrderId().value(),
                domain.deliveryAttemptId(),
                domain.schedulingMethod(),
                domain.preferredStartTime(),
                domain.preferredEndTime(),
                domain.customerPreferenceNotes(),
                domain.scheduledStartTime(),
                domain.scheduledEndTime(),
                domain.status(),
                domain.scheduledBy(),
                domain.scheduledAt(),
                domain.supersededAt(),
                domain.supersededBy(),
                domain.supersedeReason(),
                domain.createdAt(),
                domain.updatedAt()
        );
    }
}
