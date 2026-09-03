package com.transportlogistics.app.delivery.application;

import com.transportlogistics.app.delivery.domain.model.DeliveryExceptionCase;
import com.transportlogistics.app.delivery.domain.model.DeliveryExceptionStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryEscalation;
import com.transportlogistics.app.delivery.domain.model.DeliveryEscalationStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryOrder;
import com.transportlogistics.app.delivery.domain.model.DeliveryStatus;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryExceptionUseCase;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryOrderUseCase;
import com.transportlogistics.app.delivery.ports.inbound.FailedDeliveryUseCase;
import com.transportlogistics.app.delivery.ports.inbound.LastMilePlannerUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LastMilePlannerServiceTest {
    @Mock private DeliveryOrderUseCase orders;
    @Mock private FailedDeliveryUseCase failedDeliveries;
    @Mock private DeliveryExceptionUseCase exceptions;
    @Mock private DeliveryOrder delivery;
    @Mock private DeliveryExceptionCase exceptionCase;
    @Mock private DeliveryEscalation escalation;

    @Test
    void reportsPlannerContextByDelegatingOnlyToExistingDeliveryUseCases() {
        UUID deliveryId = UUID.randomUUID();
        when(delivery.status()).thenReturn(DeliveryStatus.FAILED_ATTEMPT);
        when(orders.get(deliveryId)).thenReturn(delivery);
        when(failedDeliveries.getAttemptHistory(deliveryId)).thenReturn(List.of());
        when(failedDeliveries.getEscalations(deliveryId)).thenReturn(List.of(escalation));
        when(escalation.status()).thenReturn(DeliveryEscalationStatus.OPEN);
        when(exceptions.listExceptions(deliveryId)).thenReturn(List.of(exceptionCase));
        when(exceptionCase.status()).thenReturn(DeliveryExceptionStatus.OPEN);

        LastMilePlannerUseCase.LastMilePlannerContext context = new LastMilePlannerService(
                orders, failedDeliveries, exceptions).getContext(deliveryId);

        assertThat(context.failedAttemptCount()).isZero();
        assertThat(context.activeExceptionCount()).isOne();
        assertThat(context.openEscalationCount()).isOne();
        assertThat(context.availableActions()).contains(
                LastMilePlannerUseCase.PlannerAction.SCHEDULE_REDELIVERY,
                LastMilePlannerUseCase.PlannerAction.REVIEW_SPECIALIZED_EXCEPTION);
        assertThat(context.availableActions()).doesNotContain(LastMilePlannerUseCase.PlannerAction.ESCALATE);
        verify(orders).get(deliveryId);
        verify(failedDeliveries).getAttemptHistory(deliveryId);
        verify(failedDeliveries).getEscalations(deliveryId);
        verify(exceptions).listExceptions(deliveryId);
        verifyNoMoreInteractions(orders, failedDeliveries, exceptions);
    }

    @Test
    void exposesNoPlannerMutationForTerminalDelivery() {
        UUID deliveryId = UUID.randomUUID();
        when(delivery.status()).thenReturn(DeliveryStatus.DELIVERED);
        when(orders.get(deliveryId)).thenReturn(delivery);
        when(failedDeliveries.getAttemptHistory(deliveryId)).thenReturn(List.of());
        when(failedDeliveries.getEscalations(deliveryId)).thenReturn(List.of());
        when(exceptions.listExceptions(deliveryId)).thenReturn(List.of());

        var context = new LastMilePlannerService(orders, failedDeliveries, exceptions).getContext(deliveryId);

        assertThat(context.availableActions()).isEmpty();
    }
}
