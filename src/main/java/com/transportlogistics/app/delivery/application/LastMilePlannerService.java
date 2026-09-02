package com.transportlogistics.app.delivery.application;

import com.transportlogistics.app.delivery.domain.model.DeliveryExceptionStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryStatus;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryExceptionUseCase;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryOrderUseCase;
import com.transportlogistics.app.delivery.ports.inbound.FailedDeliveryUseCase;
import com.transportlogistics.app.delivery.ports.inbound.LastMilePlannerUseCase;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** US-68 planner read model; it deliberately delegates all state changes. */
public final class LastMilePlannerService implements LastMilePlannerUseCase {
    private final DeliveryOrderUseCase orders;
    private final FailedDeliveryUseCase failedDeliveries;
    private final DeliveryExceptionUseCase exceptions;

    public LastMilePlannerService(DeliveryOrderUseCase orders, FailedDeliveryUseCase failedDeliveries,
                                  DeliveryExceptionUseCase exceptions) {
        this.orders = orders;
        this.failedDeliveries = failedDeliveries;
        this.exceptions = exceptions;
    }

    @Override
    public LastMilePlannerContext getContext(UUID deliveryOrderId) {
        var delivery = orders.get(deliveryOrderId);
        var attempts = failedDeliveries.getAttemptHistory(deliveryOrderId);
        var escalations = failedDeliveries.getEscalations(deliveryOrderId);
        var cases = exceptions.listExceptions(deliveryOrderId);
        int activeCases = (int) cases.stream()
                .filter(item -> item.status() == DeliveryExceptionStatus.OPEN
                        || item.status() == DeliveryExceptionStatus.UNDER_INVESTIGATION)
                .count();
        int openEscalations = (int) escalations.stream().filter(item -> item.status().name().equals("OPEN")
                || item.status().name().equals("UNDER_REVIEW")).count();

        return new LastMilePlannerContext(delivery, attempts.size(), activeCases, openEscalations,
                availableActions(delivery.status(), activeCases, openEscalations));
    }

    private List<PlannerAction> availableActions(DeliveryStatus status, int activeCases, int openEscalations) {
        if (status == DeliveryStatus.DELIVERED || status == DeliveryStatus.RETURN_TO_BASE) return List.of();
        var actions = new ArrayList<PlannerAction>();
        actions.add(PlannerAction.RECORD_FAILED_ATTEMPT);
        actions.add(PlannerAction.REASSIGN_RIDER);
        actions.add(PlannerAction.REVIEW_BATCH);
        actions.add(PlannerAction.RECALCULATE_ETA);
        if (activeCases > 0) actions.add(PlannerAction.REVIEW_SPECIALIZED_EXCEPTION);
        else actions.add(PlannerAction.REVIEW_WRONG_ADDRESS);
        if (openEscalations == 0) actions.add(PlannerAction.ESCALATE);
        if (status == DeliveryStatus.FAILED_ATTEMPT) actions.add(PlannerAction.SCHEDULE_REDELIVERY);
        return List.copyOf(actions);
    }
}
