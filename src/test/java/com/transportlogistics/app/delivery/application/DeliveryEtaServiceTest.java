package com.transportlogistics.app.delivery.application;

import com.transportlogistics.app.delivery.domain.model.*;
import com.transportlogistics.app.delivery.ports.outbound.*;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryEtaServiceTest {

    @Mock private DeliveryOrderRepository orderRepository;
    @Mock private DeliveryBatchRepository batchRepository;
    @Mock private DeliveryZoneRepository zoneRepository;
    @Mock private RiderEtaContextPort riderEtaContextPort;
    @Mock private DeliveryLocationLookupPort locationLookupPort;
    @Mock private LastMileRoutingPort routingPort;
    @Mock private EtaCachePort cachePort;
    @Mock private DeliveryEtaEventPublisherPort eventPublisherPort;
    @Mock private DeliveryTenantContextPort tenantContextPort;

    private DeliveryEtaService etaService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID batchId = UUID.randomUUID();
    private final UUID zoneId = UUID.randomUUID();
    private final UUID originLocId = UUID.randomUUID();
    private final UUID destLocId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        etaService = new DeliveryEtaService(
                orderRepository,
                batchRepository,
                zoneRepository,
                riderEtaContextPort,
                locationLookupPort,
                routingPort,
                cachePort,
                eventPublisherPort,
                tenantContextPort
        );
    }

    @Test
    @DisplayName("VM67-01: Should calculate single DeliveryOrder ETA with SLA evaluation")
    void calculateOrderEta_valid_succeeds() {
        when(tenantContextPort.currentTenantId()).thenReturn(Optional.of(tenantId));

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        DeliveryWindow window = new DeliveryWindow(now.minusHours(1), now.plusHours(2));

        DeliveryOrder order = DeliveryOrder.create(
                new DeliveryId(orderId),
                new DeliveryNumber("DEL-2026-000001"),
                UUID.randomUUID(),
                originLocId,
                destLocId,
                DeliveryPriority.NORMAL,
                DeliveryServiceType.STANDARD,
                window,
                "Leave at doorstep",
                now,
                "dispatcher"
        );

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(riderEtaContextPort.findForOrder(tenantId, orderId)).thenReturn(Optional.of(
                new RiderEtaContextPort.RiderEtaContext(UUID.randomUUID(), DeliveryTransportMode.BICYCLE)));
        when(cachePort.beginOrderCalculation(tenantId, orderId)).thenReturn(1L);
        when(cachePort.putOrderEtaIfCurrent(eq(tenantId), eq(orderId), eq(1L), any(), any())).thenReturn(true);
        when(locationLookupPort.findLocation(originLocId)).thenReturn(Optional.of(
                new DeliveryLocationLookupPort.LocationReference(originLocId, "ORIG", "Origin", "Address 1", 6.9271, 79.8436, true)
        ));
        when(locationLookupPort.findLocation(destLocId)).thenReturn(Optional.of(
                new DeliveryLocationLookupPort.LocationReference(destLocId, "DEST", "Destination", "Address 2", 6.8920, 79.8550, true)
        ));

        when(routingPort.estimate(any(), any(), any(), any(), any()))
                .thenReturn(new LastMileRoutingPort.RouteEstimate(5000L, 1200L, EtaSource.HEURISTIC));

        SingleOrderEtaEstimate estimate = etaService.calculateOrderEta(orderId, "dispatcher");

        assertThat(estimate.orderId()).isEqualTo(orderId);
        assertThat(estimate.travelDurationSeconds()).isEqualTo(1200L);
        assertThat(estimate.distanceMeters()).isEqualTo(5000L);
        assertThat(estimate.slaStatus()).isEqualTo(EtaStatus.ON_TIME);
        assertThat(estimate.source()).isEqualTo(EtaSource.HEURISTIC);

        verify(cachePort).putOrderEtaIfCurrent(eq(tenantId), eq(orderId), eq(1L), any(), any());
        verify(eventPublisherPort).publish(any());
    }

    @Test
    @DisplayName("VM67-02: Should calculate multi-stop batch cumulative ETA with stop service buffers")
    void calculateBatchEta_valid_succeeds() {
        when(tenantContextPort.currentTenantId()).thenReturn(Optional.of(tenantId));

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        DeliveryBatch batch = DeliveryBatch.create(
                batchId,
                tenantId,
                new DeliveryBatchCode("BAT-2026-000001"),
                zoneId,
                null,
                5,
                now,
                "dispatcher"
        );
        UUID riderId = UUID.randomUUID();
        batch = batch.assignRider(riderId, now, "dispatcher");

        DeliveryBatchOrder member1 = DeliveryBatchOrder.create(UUID.randomUUID(), tenantId, batchId, orderId, 1, now, "dispatcher");

        DeliveryZone zone = new DeliveryZone(
                zoneId,
                tenantId,
                "ZONE-01",
                "Zone One",
                "Desc",
                DeliveryZoneType.URBAN_DENSE,
                DeliveryZoneStatus.ACTIVE,
                true,
                100,
                originLocId,
                new DeliveryZoneBoundary(List.of(
                        new DeliveryZoneCoordinate(79.0, 6.0),
                        new DeliveryZoneCoordinate(80.0, 6.0),
                        new DeliveryZoneCoordinate(80.0, 7.0),
                        new DeliveryZoneCoordinate(79.0, 6.0)
                )),
                1,
                0L,
                now,
                "admin",
                now,
                "admin"
        );

        DeliveryOrder order = DeliveryOrder.create(
                new DeliveryId(orderId),
                new DeliveryNumber("DEL-2026-000001"),
                UUID.randomUUID(),
                originLocId,
                destLocId,
                DeliveryPriority.NORMAL,
                DeliveryServiceType.STANDARD,
                new DeliveryWindow(now.minusHours(1), now.plusHours(3)),
                "None",
                now,
                "dispatcher"
        );

        when(batchRepository.findById(tenantId, batchId)).thenReturn(Optional.of(batch));
        when(riderEtaContextPort.findForRider(tenantId, riderId)).thenReturn(Optional.of(
                new RiderEtaContextPort.RiderEtaContext(riderId, DeliveryTransportMode.VAN)));
        when(cachePort.beginBatchCalculation(tenantId, batchId)).thenReturn(1L);
        when(cachePort.putBatchEtaIfCurrent(eq(tenantId), eq(batchId), eq(1L), any(), any())).thenReturn(true);
        when(batchRepository.findActiveOrderMembershipsByBatchId(tenantId, batchId)).thenReturn(List.of(member1));
        when(zoneRepository.findById(zoneId, tenantId)).thenReturn(Optional.of(zone));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        when(locationLookupPort.findLocation(originLocId)).thenReturn(Optional.of(
                new DeliveryLocationLookupPort.LocationReference(originLocId, "DEPOT", "Depot", "Depot Address", 6.9271, 79.8436, true)
        ));
        when(locationLookupPort.findLocation(destLocId)).thenReturn(Optional.of(
                new DeliveryLocationLookupPort.LocationReference(destLocId, "DEST", "Dest", "Dest Address", 6.8920, 79.8550, true)
        ));

        when(routingPort.estimate(any(), any(), any(), any(), any()))
                .thenReturn(new LastMileRoutingPort.RouteEstimate(4000L, 800L, EtaSource.HEURISTIC));

        BatchEtaEstimate batchEta = etaService.calculateBatchEta(batchId, "dispatcher");

        assertThat(batchEta.batchId()).isEqualTo(batchId);
        assertThat(batchEta.stops()).hasSize(1);
        assertThat(batchEta.stops().get(0).travelDurationSeconds()).isEqualTo(800L);
        assertThat(batchEta.stops().get(0).serviceDurationSeconds()).isEqualTo(600L); // 10m for URBAN_DENSE
        assertThat(batchEta.totalDurationSeconds()).isEqualTo(1400L); // 800 travel + 600 service
        assertThat(batchEta.totalDistanceMeters()).isEqualTo(4000L);

        verify(cachePort).putBatchEtaIfCurrent(eq(tenantId), eq(batchId), eq(1L), any(), any());
        verify(eventPublisherPort).publish(any());
    }

    @Test
    @DisplayName("VM67-03: Should reject calculation if destination coordinates are missing")
    void calculateOrderEta_missingCoordinates_throws() {
        when(tenantContextPort.currentTenantId()).thenReturn(Optional.of(tenantId));

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        DeliveryOrder order = DeliveryOrder.create(
                new DeliveryId(orderId),
                new DeliveryNumber("DEL-2026-000001"),
                UUID.randomUUID(),
                originLocId,
                destLocId,
                DeliveryPriority.NORMAL,
                DeliveryServiceType.STANDARD,
                new DeliveryWindow(now, now.plusHours(1)),
                "None",
                now,
                "dispatcher"
        );

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(locationLookupPort.findLocation(originLocId)).thenReturn(Optional.of(
                new DeliveryLocationLookupPort.LocationReference(originLocId, "ORIG", "Origin", "Address 1", 6.9271, 79.8436, true)
        ));
        when(locationLookupPort.findLocation(destLocId)).thenReturn(Optional.of(
                new DeliveryLocationLookupPort.LocationReference(destLocId, "DEST", "Destination", "Address 2", null, null, true)
        ));

        assertThatThrownBy(() -> etaService.calculateOrderEta(orderId, "dispatcher"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).code()).isEqualTo("DELIVERY_ETA_COORDINATES_MISSING"));
    }

    @Test
    @DisplayName("VM67-04: Should reject cross-tenant calculation with NotFoundException")
    void calculateOrderEta_crossTenant_throwsNotFound() {
        when(tenantContextPort.currentTenantId()).thenReturn(Optional.of(tenantId));
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> etaService.calculateOrderEta(orderId, "dispatcher"))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).code()).isEqualTo("DELIVERY_ETA_SUBJECT_NOT_FOUND"));
    }
}
