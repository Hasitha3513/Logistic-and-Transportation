package com.transportlogistics.app.delivery.adapters.inbound.web.dto.response;

import com.transportlogistics.app.delivery.ports.inbound.LastMilePlannerUseCase;

import java.util.List;
import java.util.UUID;

public record LastMilePlannerContextResponse(
        UUID deliveryOrderId,
        String deliveryStatus,
        int failedAttemptCount,
        int activeExceptionCount,
        int openEscalationCount,
        List<LastMilePlannerUseCase.PlannerAction> availableActions
) {
    public static LastMilePlannerContextResponse from(LastMilePlannerUseCase.LastMilePlannerContext context) {
        return new LastMilePlannerContextResponse(
                context.delivery().id().value(), context.delivery().status().name(),
                context.failedAttemptCount(), context.activeExceptionCount(), context.openEscalationCount(),
                context.availableActions());
    }
}
