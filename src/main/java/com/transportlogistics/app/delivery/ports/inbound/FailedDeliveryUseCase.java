package com.transportlogistics.app.delivery.ports.inbound;

import com.transportlogistics.app.delivery.domain.model.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface FailedDeliveryUseCase {

    record ContactAttemptInput(
            DeliveryContactChannel channel,
            OffsetDateTime contactTimestamp,
            DeliveryContactOutcome outcome,
            String notes
    ) {}

    record RecordFailedAttemptCommand(
            long expectedVersion,
            DeliveryFailureReason failureReason,
            String notes,
            DeliveryFailureDisposition requestedDisposition,
            OffsetDateTime attemptTimestamp,
            List<ContactAttemptInput> contactAttempts
    ) {}

    record RecordContactAttemptCommand(
            DeliveryContactChannel channel,
            OffsetDateTime contactTimestamp,
            DeliveryContactOutcome outcome,
            String notes
    ) {}

    record EscalateDeliveryCommand(
            long expectedVersion,
            UUID deliveryAttemptId,
            String reason
    ) {}

    record UpdateEscalationCommand(
            DeliveryEscalationStatus status,
            String resolutionNotes,
            DeliveryFailureDisposition nextDisposition
    ) {}

    record ReturnToBaseCommand(
            long expectedVersion,
            String reason
    ) {}

    DeliveryAttempt recordFailedAttempt(UUID deliveryId, RecordFailedAttemptCommand command, String actor);

    DeliveryContactAttempt recordContactAttempt(UUID deliveryId, UUID attemptId, RecordContactAttemptCommand command, String actor);

    DeliveryEscalation escalateDelivery(UUID deliveryId, EscalateDeliveryCommand command, String actor);

    DeliveryEscalation updateEscalation(UUID deliveryId, UUID escalationId, UpdateEscalationCommand command, String actor);

    DeliveryOrder initiateReturnToBase(UUID deliveryId, ReturnToBaseCommand command, String actor);

    List<DeliveryAttempt> getAttemptHistory(UUID deliveryId);

    List<DeliveryEscalation> getEscalations(UUID deliveryId);
}
