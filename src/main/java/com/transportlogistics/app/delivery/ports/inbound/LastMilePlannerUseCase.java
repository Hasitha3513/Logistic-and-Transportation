package com.transportlogistics.app.delivery.ports.inbound;

import com.transportlogistics.app.delivery.domain.model.DeliveryOrder;

import java.util.List;
import java.util.UUID;

/**
 * Read-only US-68 planner composition. Mutations remain owned by the existing
 * failed-delivery, exception, Rider, Batch, redelivery, and ETA use cases.
 */
public interface LastMilePlannerUseCase {

    LastMilePlannerContext getContext(UUID deliveryOrderId);

    record LastMilePlannerContext(
            DeliveryOrder delivery,
            int failedAttemptCount,
            int activeExceptionCount,
            int openEscalationCount,
            List<PlannerAction> availableActions
    ) {}

    enum PlannerAction {
        RECORD_FAILED_ATTEMPT,
        REVIEW_WRONG_ADDRESS,
        REVIEW_SPECIALIZED_EXCEPTION,
        ESCALATE,
        SCHEDULE_REDELIVERY,
        REASSIGN_RIDER,
        REVIEW_BATCH,
        RECALCULATE_ETA
    }
}
