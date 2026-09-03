package com.transportlogistics.app.delivery.application;

import com.transportlogistics.app.delivery.domain.model.*;
import com.transportlogistics.app.delivery.ports.inbound.RedeliveryUseCase;
import com.transportlogistics.app.delivery.ports.outbound.*;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RedeliveryServiceTest {

    private DeliveryOrderRepository orders;
    private ProofOfDeliveryRepository proofs;
    private DeliveryAttemptRepository attempts;
    private DeliveryRedeliveryScheduleRepository schedules;
    private DeliveryTenantContextPort tenantContext;
    private DeliveryOrderTransaction transactions;
    private Clock clock;
    private RedeliveryService service;

    private final UUID tenantId = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private final UUID deliveryId = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private final UUID attemptId = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private final String actor = "delivery_manager_1";

    @BeforeEach
    void setUp() {
        orders = mock(DeliveryOrderRepository.class);
        proofs = mock(ProofOfDeliveryRepository.class);
        attempts = mock(DeliveryAttemptRepository.class);
        schedules = mock(DeliveryRedeliveryScheduleRepository.class);
        tenantContext = mock(DeliveryTenantContextPort.class);
        transactions = new DeliveryOrderTransaction() {
            @Override
            public <T> T execute(java.util.function.Supplier<T> action) {
                return action.get();
            }
        };
        // Fixed at 2026-08-31 09:00:00 UTC (14:30 in Asia/Colombo)
        clock = Clock.fixed(Instant.parse("2026-08-31T09:00:00Z"), ZoneOffset.UTC);

        when(tenantContext.currentTenant()).thenReturn(Optional.of(new DeliveryTenantContextPort.TenantContext(tenantId, "Asia/Colombo")));
        when(proofs.findByDeliveryOrderId(deliveryId)).thenReturn(Optional.empty());

        service = new RedeliveryService(orders, proofs, attempts, schedules, tenantContext, transactions, clock);
    }

    private DeliveryOrder createDeliveryOrder(DeliveryStatus status, long version) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        return new DeliveryOrder(
                new DeliveryId(deliveryId),
                new DeliveryNumber("DEL-2026-000001"),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                DeliveryPriority.NORMAL,
                DeliveryServiceType.STANDARD,
                new DeliveryWindow(now, now.plusHours(4)),
                "Leave at front desk",
                status,
                version,
                now,
                now,
                "creator",
                "creator"
        );
    }

    private DeliveryAttempt createEligibleAttempt() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        return DeliveryAttempt.create(
                attemptId,
                new DeliveryId(deliveryId),
                1,
                now,
                DeliveryFailureReason.CUSTOMER_UNAVAILABLE,
                "Customer not at home",
                DeliveryFailureDisposition.REDELIVERY_ELIGIBLE,
                null,
                actor,
                now
        );
    }

    @Test
    @DisplayName("Get suggestions generates preferred slot and next-day standard depot slots")
    void getSuggestions() {
        DeliveryOrder delivery = createDeliveryOrder(DeliveryStatus.FAILED_ATTEMPT, 1L);
        when(orders.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(schedules.countActiveOverlapping(any(), any(), any(), any())).thenReturn(10);

        OffsetDateTime prefStart = OffsetDateTime.parse("2026-09-01T04:30:00Z"); // 10:00 Asia/Colombo
        OffsetDateTime prefEnd = OffsetDateTime.parse("2026-09-01T06:30:00Z"); // 12:00 Asia/Colombo

        var input = new RedeliveryUseCase.CustomerPreferenceInput(prefStart, prefEnd, "Call before arriving");
        List<RedeliveryUseCase.RedeliverySuggestion> suggestions = service.getSuggestions(deliveryId, input);

        assertThat(suggestions).hasSize(3);
        assertThat(suggestions.get(0).slotLabel()).isEqualTo("Preferred Window");
        assertThat(suggestions.get(0).available()).isTrue();
        assertThat(suggestions.get(1).slotLabel()).contains("Next-Day Morning");
        assertThat(suggestions.get(2).slotLabel()).contains("Next-Day Afternoon");
    }

    @Test
    @DisplayName("Schedule redelivery transitions order to READY_FOR_ASSIGNMENT and saves confirmed schedule")
    void scheduleRedeliverySuccess() {
        DeliveryOrder delivery = createDeliveryOrder(DeliveryStatus.FAILED_ATTEMPT, 2L);
        when(orders.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(attempts.findByDeliveryId(deliveryId)).thenReturn(List.of(createEligibleAttempt()));
        when(schedules.countActiveOverlapping(any(), any(), any(), any())).thenReturn(5);
        when(schedules.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OffsetDateTime schedStart = OffsetDateTime.parse("2026-09-01T04:30:00Z"); // 10:00 Asia/Colombo
        OffsetDateTime schedEnd = OffsetDateTime.parse("2026-09-01T08:30:00Z"); // 14:00 Asia/Colombo

        var command = new RedeliveryUseCase.ScheduleRedeliveryCommand(
                2L,
                attemptId,
                RedeliverySchedulingMethod.AGENT_ASSISTED,
                schedStart,
                schedEnd,
                "Customer requested morning",
                schedStart,
                schedEnd
        );

        DeliveryRedeliverySchedule result = service.scheduleRedelivery(deliveryId, command, actor);

        assertThat(result.status()).isEqualTo(RedeliveryScheduleStatus.CONFIRMED);
        assertThat(result.scheduledStartTime()).isEqualTo(schedStart);
        assertThat(result.scheduledEndTime()).isEqualTo(schedEnd);
        assertThat(result.scheduledBy()).isEqualTo(actor);

        ArgumentCaptor<DeliveryOrder> orderCaptor = ArgumentCaptor.forClass(DeliveryOrder.class);
        verify(orders).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().status()).isEqualTo(DeliveryStatus.READY_FOR_ASSIGNMENT);
        assertThat(orderCaptor.getValue().window().start()).isEqualTo(schedStart);
    }

    @Test
    @DisplayName("Schedule redelivery rejected when capacity limit (50) is reached")
    void scheduleRedeliveryCapacityExceeded() {
        DeliveryOrder delivery = createDeliveryOrder(DeliveryStatus.FAILED_ATTEMPT, 1L);
        when(orders.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(attempts.findByDeliveryId(deliveryId)).thenReturn(List.of(createEligibleAttempt()));
        when(schedules.countActiveOverlapping(any(), any(), any(), any())).thenReturn(50);

        OffsetDateTime schedStart = OffsetDateTime.parse("2026-09-01T04:30:00Z");
        OffsetDateTime schedEnd = OffsetDateTime.parse("2026-09-01T08:30:00Z");

        var command = new RedeliveryUseCase.ScheduleRedeliveryCommand(
                1L, attemptId, RedeliverySchedulingMethod.AUTOMATIC, null, null, null, schedStart, schedEnd
        );

        assertThatThrownBy(() -> service.scheduleRedelivery(deliveryId, command, actor))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Operational delivery slot capacity exceeded");
    }

    @Test
    @DisplayName("Schedule redelivery rejected when window is outside business hours")
    void scheduleRedeliveryOutOfBusinessHours() {
        DeliveryOrder delivery = createDeliveryOrder(DeliveryStatus.FAILED_ATTEMPT, 1L);
        when(orders.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(attempts.findByDeliveryId(deliveryId)).thenReturn(List.of(createEligibleAttempt()));

        // 21:00 to 23:00 in Asia/Colombo (15:30 to 17:30 UTC) -> After 20:00 Colombo
        OffsetDateTime schedStart = OffsetDateTime.parse("2026-09-01T15:30:00Z");
        OffsetDateTime schedEnd = OffsetDateTime.parse("2026-09-01T17:30:00Z");

        var command = new RedeliveryUseCase.ScheduleRedeliveryCommand(
                1L, attemptId, RedeliverySchedulingMethod.AGENT_ASSISTED, null, null, null, schedStart, schedEnd
        );

        assertThatThrownBy(() -> service.scheduleRedelivery(deliveryId, command, actor))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("within depot operational hours (08:00 to 20:00)");
    }

    @Test
    @DisplayName("Schedule redelivery rejected on stale version")
    void scheduleRedeliveryStaleVersion() {
        DeliveryOrder delivery = createDeliveryOrder(DeliveryStatus.FAILED_ATTEMPT, 5L);
        when(orders.findById(deliveryId)).thenReturn(Optional.of(delivery));

        OffsetDateTime schedStart = OffsetDateTime.parse("2026-09-01T04:30:00Z");
        OffsetDateTime schedEnd = OffsetDateTime.parse("2026-09-01T08:30:00Z");

        var command = new RedeliveryUseCase.ScheduleRedeliveryCommand(
                4L, attemptId, RedeliverySchedulingMethod.AGENT_ASSISTED, null, null, null, schedStart, schedEnd
        );

        assertThatThrownBy(() -> service.scheduleRedelivery(deliveryId, command, actor))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Stale delivery order version");
    }

    @Test
    @DisplayName("Schedule redelivery rejected if order is DELIVERED")
    void scheduleRedeliveryDeliveredRejected() {
        DeliveryOrder delivery = createDeliveryOrder(DeliveryStatus.DELIVERED, 1L);
        when(orders.findById(deliveryId)).thenReturn(Optional.of(delivery));

        OffsetDateTime schedStart = OffsetDateTime.parse("2026-09-01T04:30:00Z");
        OffsetDateTime schedEnd = OffsetDateTime.parse("2026-09-01T08:30:00Z");

        var command = new RedeliveryUseCase.ScheduleRedeliveryCommand(
                1L, attemptId, RedeliverySchedulingMethod.AGENT_ASSISTED, null, null, null, schedStart, schedEnd
        );

        assertThatThrownBy(() -> service.scheduleRedelivery(deliveryId, command, actor))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot schedule re-delivery for a delivered order");
    }

    @Test
    @DisplayName("Schedule redelivery rejected if order is RETURN_TO_BASE")
    void scheduleRedeliveryReturnToBaseRejected() {
        DeliveryOrder delivery = createDeliveryOrder(DeliveryStatus.RETURN_TO_BASE, 1L);
        when(orders.findById(deliveryId)).thenReturn(Optional.of(delivery));

        OffsetDateTime schedStart = OffsetDateTime.parse("2026-09-01T04:30:00Z");
        OffsetDateTime schedEnd = OffsetDateTime.parse("2026-09-01T08:30:00Z");

        var command = new RedeliveryUseCase.ScheduleRedeliveryCommand(
                1L, attemptId, RedeliverySchedulingMethod.AGENT_ASSISTED, null, null, null, schedStart, schedEnd
        );

        assertThatThrownBy(() -> service.scheduleRedelivery(deliveryId, command, actor))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot schedule re-delivery for an order returned to base");
    }

    @Test
    @DisplayName("Reschedule supersedes old schedule and persists new confirmed schedule")
    void rescheduleSuccess() {
        DeliveryOrder delivery = createDeliveryOrder(DeliveryStatus.READY_FOR_ASSIGNMENT, 3L);
        when(orders.findById(deliveryId)).thenReturn(Optional.of(delivery));

        OffsetDateTime oldStart = OffsetDateTime.parse("2026-09-01T04:30:00Z");
        OffsetDateTime oldEnd = OffsetDateTime.parse("2026-09-01T08:30:00Z");
        DeliveryRedeliverySchedule existing = DeliveryRedeliverySchedule.createConfirmed(
                UUID.randomUUID(), tenantId, new DeliveryId(deliveryId), attemptId,
                RedeliverySchedulingMethod.AGENT_ASSISTED, null, null, null,
                oldStart, oldEnd, "earlier_operator", OffsetDateTime.now(clock).minusHours(1)
        );

        when(schedules.findCurrentConfirmed(deliveryId)).thenReturn(Optional.of(existing));
        when(schedules.countActiveOverlapping(any(), any(), any(), any())).thenReturn(10);
        when(schedules.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OffsetDateTime newStart = OffsetDateTime.parse("2026-09-02T04:30:00Z");
        OffsetDateTime newEnd = OffsetDateTime.parse("2026-09-02T08:30:00Z");

        var command = new RedeliveryUseCase.RescheduleRedeliveryCommand(
                3L, "Customer requested delay to next day", newStart, newEnd
        );

        DeliveryRedeliverySchedule result = service.reschedule(deliveryId, command, actor);

        assertThat(existing.status()).isEqualTo(RedeliveryScheduleStatus.SUPERSEDED);
        assertThat(existing.supersedeReason()).isEqualTo("Customer requested delay to next day");
        assertThat(result.status()).isEqualTo(RedeliveryScheduleStatus.CONFIRMED);
        assertThat(result.scheduledStartTime()).isEqualTo(newStart);

        verify(schedules, times(2)).save(any());
        verify(orders).save(any());
    }

    @Test
    @DisplayName("Get history returns all schedules for order")
    void getHistory() {
        DeliveryOrder delivery = createDeliveryOrder(DeliveryStatus.READY_FOR_ASSIGNMENT, 1L);
        when(orders.findById(deliveryId)).thenReturn(Optional.of(delivery));

        DeliveryRedeliverySchedule sched = mock(DeliveryRedeliverySchedule.class);
        when(schedules.findByDeliveryOrderId(deliveryId)).thenReturn(List.of(sched));

        List<DeliveryRedeliverySchedule> history = service.getHistory(deliveryId);
        assertThat(history).hasSize(1);
    }
}
