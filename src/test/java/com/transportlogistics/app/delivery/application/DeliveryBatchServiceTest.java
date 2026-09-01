package com.transportlogistics.app.delivery.application;

import com.transportlogistics.app.delivery.domain.model.DeliveryBatch;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatchCode;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatchOrder;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatchOrderStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatchStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryId;
import com.transportlogistics.app.delivery.domain.model.DeliveryNumber;
import com.transportlogistics.app.delivery.domain.model.DeliveryOrder;
import com.transportlogistics.app.delivery.domain.model.DeliveryPriority;
import com.transportlogistics.app.delivery.domain.model.DeliveryRider;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderType;
import com.transportlogistics.app.delivery.domain.model.DeliveryServiceType;
import com.transportlogistics.app.delivery.domain.model.DeliveryStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryWindow;
import com.transportlogistics.app.delivery.domain.model.DeliveryZone;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneBoundary;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneCoordinate;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneType;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryBatchUseCase;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryZoneLookupPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryBatchCodeGenerator;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryBatchEventPublisherPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryBatchRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryOrderRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryOrderTransaction;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryRiderRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort;
import com.transportlogistics.app.delivery.ports.outbound.DriverEligibilityPort;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryBatchServiceTest {

    @Mock private DeliveryBatchRepository batchRepository;
    @Mock private DeliveryOrderRepository orderRepository;
    @Mock private DeliveryRiderRepository riderRepository;
    @Mock private DriverEligibilityPort driverEligibilityPort;
    @Mock private DeliveryZoneLookupPort zoneLookupPort;
    @Mock private DeliveryTenantContextPort tenantContext;
    @Mock private DeliveryBatchCodeGenerator codeGenerator;
    @Mock private DeliveryBatchEventPublisherPort eventPublisher;

    private final DeliveryOrderTransaction transactions = new DeliveryOrderTransaction() {
        @Override
        public <T> T execute(Supplier<T> operation) {
            return operation.get();
        }
    };

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-01T10:00:00Z"), ZoneOffset.UTC);
    private DeliveryBatchService service;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID zoneId = UUID.randomUUID();
    private final UUID slotId = UUID.randomUUID();
    private final UUID riderId = UUID.randomUUID();
    private final UUID driverId = UUID.randomUUID();
    private final UUID orderId1 = UUID.randomUUID();
    private final UUID orderId2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(tenantContext.currentTenantId()).thenReturn(Optional.of(tenantId));
        service = new DeliveryBatchService(
                batchRepository,
                orderRepository,
                riderRepository,
                driverEligibilityPort,
                zoneLookupPort,
                tenantContext,
                transactions,
                codeGenerator,
                eventPublisher,
                clock
        );
    }

    private DeliveryZone createZone(UUID id, DeliveryZoneStatus status) {
        return new DeliveryZone(
                id, tenantId, "ZONE-01", "Downtown", "Test Zone",
                DeliveryZoneType.URBAN_DENSE, status, true, 100, null,
                new DeliveryZoneBoundary(List.of(
                        new DeliveryZoneCoordinate(10.0, 10.0),
                        new DeliveryZoneCoordinate(10.0, 20.0),
                        new DeliveryZoneCoordinate(20.0, 20.0),
                        new DeliveryZoneCoordinate(20.0, 10.0),
                        new DeliveryZoneCoordinate(10.0, 10.0)
                )),
                0, 0L, OffsetDateTime.now(clock), "admin", OffsetDateTime.now(clock), "admin"
        );
    }

    private DeliveryOrder createOrder(UUID id, UUID locId, DeliveryStatus status) {
        return new DeliveryOrder(
                new DeliveryId(id),
                new DeliveryNumber("DEL-2026-000001"),
                UUID.randomUUID(),
                UUID.randomUUID(),
                locId,
                DeliveryPriority.NORMAL,
                DeliveryServiceType.STANDARD,
                new DeliveryWindow(OffsetDateTime.now(clock), OffsetDateTime.now(clock).plusHours(2)),
                "Leave at front door",
                status,
                0L,
                OffsetDateTime.now(clock),
                OffsetDateTime.now(clock),
                "admin",
                "admin"
        );
    }

    @Test
    @DisplayName("Should create manual batch with valid ready orders in same zone")
    void createBatch_manualValid_succeeds() {
        when(zoneLookupPort.findZone(zoneId)).thenReturn(Optional.of(createZone(zoneId, DeliveryZoneStatus.ACTIVE)));
        when(codeGenerator.next(eq(tenantId), any())).thenReturn(new DeliveryBatchCode("BAT-2026-000001"));
        when(batchRepository.save(any(DeliveryBatch.class))).thenAnswer(i -> i.getArgument(0));

        UUID destLoc1 = UUID.randomUUID();
        UUID destLoc2 = UUID.randomUUID();
        when(orderRepository.findByIdForUpdate(orderId1)).thenReturn(Optional.of(createOrder(orderId1, destLoc1, DeliveryStatus.READY_FOR_ASSIGNMENT)));
        when(orderRepository.findByIdForUpdate(orderId2)).thenReturn(Optional.of(createOrder(orderId2, destLoc2, DeliveryStatus.READY_FOR_ASSIGNMENT)));
        when(zoneLookupPort.resolveZoneForLocation(destLoc1)).thenReturn(Optional.of(createZone(zoneId, DeliveryZoneStatus.ACTIVE)));
        when(zoneLookupPort.resolveZoneForLocation(destLoc2)).thenReturn(Optional.of(createZone(zoneId, DeliveryZoneStatus.ACTIVE)));
        when(batchRepository.findActiveBatchedDeliveryOrderIds(eq(tenantId), any())).thenReturn(List.of());

        DeliveryBatch batch = service.createBatch(new DeliveryBatchUseCase.CreateDeliveryBatchCommand(
                zoneId, slotId, 5, List.of(orderId1, orderId2), null
        ));

        assertThat(batch).isNotNull();
        assertThat(batch.deliveryZoneId()).isEqualTo(zoneId);
        assertThat(batch.status()).isEqualTo(DeliveryBatchStatus.DRAFT);
        verify(batchRepository).save(any(DeliveryBatch.class));
    }

    @Test
    @DisplayName("Should reject batch creation if an order is already in an active batch")
    void createBatch_alreadyBatchedOrder_throwsConflict() {
        when(zoneLookupPort.findZone(zoneId)).thenReturn(Optional.of(createZone(zoneId, DeliveryZoneStatus.ACTIVE)));
        when(codeGenerator.next(eq(tenantId), any())).thenReturn(new DeliveryBatchCode("BAT-2026-000002"));
        when(batchRepository.save(any(DeliveryBatch.class))).thenAnswer(i -> i.getArgument(0));
        when(batchRepository.findActiveBatchedDeliveryOrderIds(eq(tenantId), any())).thenReturn(List.of(orderId1));

        assertThatThrownBy(() -> service.createBatch(new DeliveryBatchUseCase.CreateDeliveryBatchCommand(
                zoneId, slotId, 5, List.of(orderId1), null
        ))).isInstanceOf(ConflictException.class)
          .hasMessageContaining("already in an active batch");
    }

    @Test
    @DisplayName("Should reject batch creation if order destination zone does not match batch zone")
    void createBatch_zoneMismatch_throwsConflict() {
        when(zoneLookupPort.findZone(zoneId)).thenReturn(Optional.of(createZone(zoneId, DeliveryZoneStatus.ACTIVE)));
        when(codeGenerator.next(eq(tenantId), any())).thenReturn(new DeliveryBatchCode("BAT-2026-000003"));
        when(batchRepository.save(any(DeliveryBatch.class))).thenAnswer(i -> i.getArgument(0));

        UUID destLoc1 = UUID.randomUUID();
        when(orderRepository.findByIdForUpdate(orderId1)).thenReturn(Optional.of(createOrder(orderId1, destLoc1, DeliveryStatus.READY_FOR_ASSIGNMENT)));
        UUID otherZoneId = UUID.randomUUID();
        when(zoneLookupPort.resolveZoneForLocation(destLoc1)).thenReturn(Optional.of(createZone(otherZoneId, DeliveryZoneStatus.ACTIVE)));
        when(batchRepository.findActiveBatchedDeliveryOrderIds(eq(tenantId), any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.createBatch(new DeliveryBatchUseCase.CreateDeliveryBatchCommand(
                zoneId, slotId, 5, List.of(orderId1), null
        ))).isInstanceOf(ConflictException.class)
          .hasMessageContaining("does not match batch zone");
    }

    @Test
    @DisplayName("Should assign eligible rider and update batch status to ASSIGNED")
    void assignRider_validRider_succeeds() {
        DeliveryBatch batch = DeliveryBatch.create(UUID.randomUUID(), tenantId, new DeliveryBatchCode("BAT-2026-000004"), zoneId, slotId, 5, OffsetDateTime.now(clock), "admin");
        when(batchRepository.findByIdForUpdate(tenantId, batch.id())).thenReturn(Optional.of(batch));

        DeliveryRider rider = DeliveryRider.create(
                riderId, tenantId, "RDR-001", driverId, DeliveryRiderType.FULL_TIME, zoneId, Set.of(), 5, "admin", OffsetDateTime.now(clock)
        );
        when(riderRepository.findByIdForUpdate(riderId, tenantId)).thenReturn(Optional.of(rider));
        when(driverEligibilityPort.findDriver(driverId)).thenReturn(Optional.of(new DriverEligibilityPort.DriverSummary(
                driverId, "EMP-123", "John", "Doe", "AVAILABLE", true
        )));
        when(batchRepository.countActiveOrdersByBatchId(tenantId, batch.id())).thenReturn(2);
        when(riderRepository.countActiveAssignmentsForRider(riderId, tenantId)).thenReturn(1);
        when(batchRepository.findActiveOrderMembershipsByBatchId(tenantId, batch.id())).thenReturn(List.of());
        when(batchRepository.save(any(DeliveryBatch.class))).thenAnswer(i -> i.getArgument(0));

        DeliveryBatch assigned = service.assignRider(batch.id(), new DeliveryBatchUseCase.AssignRiderToBatchCommand(
                riderId, false, null
        ));

        assertThat(assigned.status()).isEqualTo(DeliveryBatchStatus.ASSIGNED);
        assertThat(assigned.riderId()).isEqualTo(riderId);
    }
}
