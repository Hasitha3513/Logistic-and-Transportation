package com.transportlogistics.app.delivery.application;

import com.transportlogistics.app.delivery.domain.model.*;
import com.transportlogistics.app.delivery.ports.inbound.FailedDeliveryUseCase;
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

class FailedDeliveryServiceTest {

    private DeliveryOrderRepository orders;
    private ProofOfDeliveryRepository proofs;
    private DeliveryAttemptRepository attempts;
    private DeliveryContactAttemptRepository contactAttempts;
    private DeliveryEscalationRepository escalations;
    private DeliveryTenantContextPort tenantContext;
    private DeliveryOrderTransaction transactions;
    private Clock clock;
    private FailedDeliveryService service;

    private final UUID tenantId = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private final UUID deliveryId = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private final String actor = "delivery_manager_1";

    @BeforeEach
    void setUp() {
        orders = mock(DeliveryOrderRepository.class);
        proofs = mock(ProofOfDeliveryRepository.class);
        attempts = mock(DeliveryAttemptRepository.class);
        contactAttempts = mock(DeliveryContactAttemptRepository.class);
        escalations = mock(DeliveryEscalationRepository.class);
        tenantContext = mock(DeliveryTenantContextPort.class);
        transactions = new DeliveryOrderTransaction() {
            @Override
            public <T> T execute(java.util.function.Supplier<T> action) {
                return action.get();
            }
        };
        clock = Clock.fixed(Instant.parse("2026-08-31T10:00:00Z"), ZoneOffset.UTC);

        when(tenantContext.currentTenant()).thenReturn(Optional.of(new DeliveryTenantContextPort.TenantContext(tenantId, "UTC")));
        when(proofs.findByDeliveryOrderId(deliveryId)).thenReturn(Optional.empty());

        service = new FailedDeliveryService(orders, proofs, attempts, contactAttempts, escalations, tenantContext, transactions, clock);
    }

    private DeliveryOrder createReadyDeliveryOrder(long version) {
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
                "Instructions",
                DeliveryStatus.READY_FOR_ASSIGNMENT,
                version,
                now,
                now,
                "creator",
                "creator"
        );
    }

    @Test
    @DisplayName("Record failed attempt with CUSTOMER_UNAVAILABLE transitions delivery to FAILED_ATTEMPT and increments attempt number")
    void recordFailedAttemptCustomerUnavailable() {
        DeliveryOrder delivery = createReadyDeliveryOrder(1L);
        when(orders.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(attempts.countByDeliveryId(deliveryId)).thenReturn(0);
        when(attempts.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var command = new FailedDeliveryUseCase.RecordFailedAttemptCommand(
                1L,
                DeliveryFailureReason.CUSTOMER_UNAVAILABLE,
                "Customer not present at delivery address",
                null,
                null,
                List.of(new FailedDeliveryUseCase.ContactAttemptInput(
                        DeliveryContactChannel.PHONE, null, DeliveryContactOutcome.NO_ANSWER, "Left message"))
        );

        DeliveryAttempt result = service.recordFailedAttempt(deliveryId, command, actor);

        assertThat(result.attemptNumber()).isEqualTo(1);
        assertThat(result.failureReason()).isEqualTo(DeliveryFailureReason.CUSTOMER_UNAVAILABLE);
        assertThat(result.disposition()).isEqualTo(DeliveryFailureDisposition.REDELIVERY_ELIGIBLE);

        ArgumentCaptor<DeliveryOrder> orderCaptor = ArgumentCaptor.forClass(DeliveryOrder.class);
        verify(orders).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().status()).isEqualTo(DeliveryStatus.FAILED_ATTEMPT);

        verify(contactAttempts).saveAll(any());
    }

    @Test
    @DisplayName("Record failed attempt with CUSTOMER_REFUSED transitions delivery to RETURN_TO_BASE")
    void recordFailedAttemptCustomerRefused() {
        DeliveryOrder delivery = createReadyDeliveryOrder(2L);
        when(orders.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(attempts.countByDeliveryId(deliveryId)).thenReturn(1);
        when(attempts.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var command = new FailedDeliveryUseCase.RecordFailedAttemptCommand(
                2L,
                DeliveryFailureReason.CUSTOMER_REFUSED,
                "Customer rejected cargo claiming order cancelled",
                null,
                null,
                List.of()
        );

        DeliveryAttempt result = service.recordFailedAttempt(deliveryId, command, actor);

        assertThat(result.attemptNumber()).isEqualTo(2);
        assertThat(result.disposition()).isEqualTo(DeliveryFailureDisposition.RETURN_TO_BASE_REQUIRED);

        ArgumentCaptor<DeliveryOrder> orderCaptor = ArgumentCaptor.forClass(DeliveryOrder.class);
        verify(orders).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().status()).isEqualTo(DeliveryStatus.RETURN_TO_BASE);
    }

    @Test
    @DisplayName("Record failed attempt with DAMAGED_CARGO transitions to ESCALATED and creates DeliveryEscalation")
    void recordFailedAttemptDamagedCargo() {
        DeliveryOrder delivery = createReadyDeliveryOrder(1L);
        when(orders.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(attempts.countByDeliveryId(deliveryId)).thenReturn(0);
        when(attempts.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var command = new FailedDeliveryUseCase.RecordFailedAttemptCommand(
                1L,
                DeliveryFailureReason.DAMAGED_CARGO,
                "Package torn and leaking fluid",
                null,
                null,
                List.of()
        );

        DeliveryAttempt result = service.recordFailedAttempt(deliveryId, command, actor);

        assertThat(result.disposition()).isEqualTo(DeliveryFailureDisposition.ESCALATED);

        ArgumentCaptor<DeliveryOrder> orderCaptor = ArgumentCaptor.forClass(DeliveryOrder.class);
        verify(orders).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().status()).isEqualTo(DeliveryStatus.ESCALATED);

        verify(escalations).save(any());
    }

    @Test
    @DisplayName("Reject recording failed attempt if Delivery is already DELIVERED")
    void rejectIfAlreadyDelivered() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        DeliveryOrder delivered = new DeliveryOrder(
                new DeliveryId(deliveryId), new DeliveryNumber("DEL-2026-000001"),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                DeliveryPriority.NORMAL, DeliveryServiceType.STANDARD,
                new DeliveryWindow(now, now.plusHours(4)), null,
                DeliveryStatus.DELIVERED, 3L, now, now, "creator", "creator");

        when(orders.findById(deliveryId)).thenReturn(Optional.of(delivered));

        var command = new FailedDeliveryUseCase.RecordFailedAttemptCommand(
                3L, DeliveryFailureReason.CUSTOMER_UNAVAILABLE, "Notes", null, null, List.of());

        assertThatThrownBy(() -> service.recordFailedAttempt(deliveryId, command, actor))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot record failure for a delivered order");
    }

    @Test
    @DisplayName("Reject recording failed attempt if POD is already FINALIZED")
    void rejectIfPodFinalized() {
        DeliveryOrder delivery = createReadyDeliveryOrder(1L);
        when(orders.findById(deliveryId)).thenReturn(Optional.of(delivery));

        ProofOfDelivery finalizedPod = mock(ProofOfDelivery.class);
        when(finalizedPod.status()).thenReturn(PodStatus.FINALIZED);
        when(proofs.findByDeliveryOrderId(deliveryId)).thenReturn(Optional.of(finalizedPod));

        var command = new FailedDeliveryUseCase.RecordFailedAttemptCommand(
                1L, DeliveryFailureReason.CUSTOMER_UNAVAILABLE, "Notes", null, null, List.of());

        assertThatThrownBy(() -> service.recordFailedAttempt(deliveryId, command, actor))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Proof of delivery has already been finalized");
    }

    @Test
    @DisplayName("Reject recording failed attempt if expectedVersion does not match current version")
    void rejectStaleVersion() {
        DeliveryOrder delivery = createReadyDeliveryOrder(5L);
        when(orders.findById(deliveryId)).thenReturn(Optional.of(delivery));

        var command = new FailedDeliveryUseCase.RecordFailedAttemptCommand(
                4L, DeliveryFailureReason.CUSTOMER_UNAVAILABLE, "Notes", null, null, List.of());

        assertThatThrownBy(() -> service.recordFailedAttempt(deliveryId, command, actor))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Stale delivery order version");
    }

    @Test
    @DisplayName("Initiate Return to Base transitions delivery to RETURN_TO_BASE")
    void initiateReturnToBase() {
        DeliveryOrder delivery = createReadyDeliveryOrder(1L);
        when(orders.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(orders.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var command = new FailedDeliveryUseCase.ReturnToBaseCommand(1L, "Depot manager ordered return");
        DeliveryOrder updated = service.initiateReturnToBase(deliveryId, command, actor);

        assertThat(updated.status()).isEqualTo(DeliveryStatus.RETURN_TO_BASE);
    }

    @Test
    @DisplayName("Secondary transition: FAILED_ATTEMPT to ESCALATED via escalateDelivery")
    void escalateDeliveryFromFailedAttempt() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        DeliveryOrder failedOrder = new DeliveryOrder(
                new DeliveryId(deliveryId), new DeliveryNumber("DEL-2026-000001"),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                DeliveryPriority.NORMAL, DeliveryServiceType.STANDARD,
                new DeliveryWindow(now, now.plusHours(4)), null,
                DeliveryStatus.FAILED_ATTEMPT, 2L, now, now, "creator", "creator");

        when(orders.findById(deliveryId)).thenReturn(Optional.of(failedOrder));
        when(escalations.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var command = new FailedDeliveryUseCase.EscalateDeliveryCommand(2L, null, "Customer dispute regarding damaged cargo packaging");
        DeliveryEscalation esc = service.escalateDelivery(deliveryId, command, actor);

        assertThat(esc.status()).isEqualTo(DeliveryEscalationStatus.OPEN);
        assertThat(esc.reason()).isEqualTo("Customer dispute regarding damaged cargo packaging");

        ArgumentCaptor<DeliveryOrder> captor = ArgumentCaptor.forClass(DeliveryOrder.class);
        verify(orders).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(DeliveryStatus.ESCALATED);
    }

    @Test
    @DisplayName("Secondary transition: Resolve escalation to FAILED_ATTEMPT (REDELIVERY_ELIGIBLE)")
    void resolveEscalationToFailedAttempt() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        DeliveryOrder escalatedOrder = new DeliveryOrder(
                new DeliveryId(deliveryId), new DeliveryNumber("DEL-2026-000001"),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                DeliveryPriority.NORMAL, DeliveryServiceType.STANDARD,
                new DeliveryWindow(now, now.plusHours(4)), null,
                DeliveryStatus.ESCALATED, 3L, now, now, "creator", "creator");

        UUID escId = UUID.randomUUID();
        DeliveryEscalation openEsc = DeliveryEscalation.create(escId, new DeliveryId(deliveryId), null, "Investigation required", actor, now);

        when(orders.findById(deliveryId)).thenReturn(Optional.of(escalatedOrder));
        when(escalations.findById(escId)).thenReturn(Optional.of(openEsc));
        when(escalations.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var command = new FailedDeliveryUseCase.UpdateEscalationCommand(
                DeliveryEscalationStatus.RESOLVED,
                "Customer agreed to receive tomorrow morning",
                DeliveryFailureDisposition.REDELIVERY_ELIGIBLE
        );

        DeliveryEscalation resolved = service.updateEscalation(deliveryId, escId, command, actor);

        assertThat(resolved.status()).isEqualTo(DeliveryEscalationStatus.RESOLVED);
        assertThat(resolved.resolutionNotes()).isEqualTo("Customer agreed to receive tomorrow morning");

        ArgumentCaptor<DeliveryOrder> captor = ArgumentCaptor.forClass(DeliveryOrder.class);
        verify(orders).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(DeliveryStatus.FAILED_ATTEMPT);
    }

    @Test
    @DisplayName("Secondary transition: Resolve escalation to RETURN_TO_BASE")
    void resolveEscalationToReturnToBase() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        DeliveryOrder escalatedOrder = new DeliveryOrder(
                new DeliveryId(deliveryId), new DeliveryNumber("DEL-2026-000001"),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                DeliveryPriority.NORMAL, DeliveryServiceType.STANDARD,
                new DeliveryWindow(now, now.plusHours(4)), null,
                DeliveryStatus.ESCALATED, 3L, now, now, "creator", "creator");

        UUID escId = UUID.randomUUID();
        DeliveryEscalation openEsc = DeliveryEscalation.create(escId, new DeliveryId(deliveryId), null, "Severe damage report", actor, now);

        when(orders.findById(deliveryId)).thenReturn(Optional.of(escalatedOrder));
        when(escalations.findById(escId)).thenReturn(Optional.of(openEsc));
        when(escalations.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var command = new FailedDeliveryUseCase.UpdateEscalationCommand(
                DeliveryEscalationStatus.RESOLVED,
                "Cargo unusable, confirmed return to base",
                DeliveryFailureDisposition.RETURN_TO_BASE_REQUIRED
        );

        DeliveryEscalation resolved = service.updateEscalation(deliveryId, escId, command, actor);

        assertThat(resolved.status()).isEqualTo(DeliveryEscalationStatus.RESOLVED);

        ArgumentCaptor<DeliveryOrder> captor = ArgumentCaptor.forClass(DeliveryOrder.class);
        verify(orders).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(DeliveryStatus.RETURN_TO_BASE);
    }

    @Test
    @DisplayName("Reject operation when Tenant context is absent")
    void rejectWhenTenantAbsent() {
        when(tenantContext.currentTenant()).thenReturn(Optional.empty());

        var command = new FailedDeliveryUseCase.RecordFailedAttemptCommand(
                1L, DeliveryFailureReason.CUSTOMER_UNAVAILABLE, "Notes", null, null, List.of());

        assertThatThrownBy(() -> service.recordFailedAttempt(deliveryId, command, actor))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Tenant context is required");
    }
}
