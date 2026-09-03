package com.transportlogistics.app.delivery.ports.inbound;

import com.transportlogistics.app.delivery.domain.model.DeliveryRedeliverySchedule;
import com.transportlogistics.app.delivery.domain.model.RedeliverySchedulingMethod;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Inbound port for managing delivery re-delivery scheduling and history.
 */
public interface RedeliveryUseCase {

    record CustomerPreferenceInput(
            OffsetDateTime preferredStartTime,
            OffsetDateTime preferredEndTime,
            String customerPreferenceNotes
    ) {}

    record RedeliverySuggestion(
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            String slotLabel,
            boolean available,
            String note
    ) {}

    record ScheduleRedeliveryCommand(
            long expectedVersion,
            UUID failedAttemptId,
            RedeliverySchedulingMethod schedulingMethod,
            OffsetDateTime preferredStartTime,
            OffsetDateTime preferredEndTime,
            String customerPreferenceNotes,
            OffsetDateTime scheduledStartTime,
            OffsetDateTime scheduledEndTime
    ) {}

    record RescheduleRedeliveryCommand(
            long expectedVersion,
            String supersedeReason,
            OffsetDateTime scheduledStartTime,
            OffsetDateTime scheduledEndTime
    ) {}

    List<RedeliverySuggestion> getSuggestions(UUID deliveryId, CustomerPreferenceInput preference);

    DeliveryRedeliverySchedule scheduleRedelivery(UUID deliveryId, ScheduleRedeliveryCommand command, String actor);

    DeliveryRedeliverySchedule reschedule(UUID deliveryId, RescheduleRedeliveryCommand command, String actor);

    List<DeliveryRedeliverySchedule> getHistory(UUID deliveryId);
}
