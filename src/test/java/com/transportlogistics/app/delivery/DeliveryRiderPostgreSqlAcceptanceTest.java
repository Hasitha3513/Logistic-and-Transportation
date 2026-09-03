package com.transportlogistics.app.delivery;

import com.transportlogistics.app.delivery.domain.model.DeliveryId;
import com.transportlogistics.app.delivery.domain.model.DeliveryNumber;
import com.transportlogistics.app.delivery.domain.model.DeliveryOrder;
import com.transportlogistics.app.delivery.domain.model.DeliveryOrderRiderAssignment;
import com.transportlogistics.app.delivery.domain.model.DeliveryPriority;
import com.transportlogistics.app.delivery.domain.model.DeliveryRider;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderShift;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderType;
import com.transportlogistics.app.delivery.domain.model.DeliveryTransportMode;
import com.transportlogistics.app.delivery.domain.model.DeliveryServiceType;
import com.transportlogistics.app.delivery.domain.model.DeliveryStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryWindow;
import com.transportlogistics.app.delivery.domain.model.DeliveryZone;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneCoordinate;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryOrderRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryRiderRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class DeliveryRiderPostgreSqlAcceptanceTest {

    private static final UUID TENANT_A = com.transportlogistics.app.tenancy.CanonicalTenant.ID;
    private static final UUID TENANT_B = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Autowired
    private DeliveryRiderRepository riderAdapter;

    @Autowired
    private DeliveryZoneRepository zoneAdapter;

    @Autowired
    private DeliveryOrderRepository orderAdapter;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort tenantContext;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate txTemplate;
    private final OffsetDateTime now = OffsetDateTime.now();
    private UUID zoneAId;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.when(tenantContext.currentTenant())
                .thenReturn(Optional.of(new com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort.TenantContext(TENANT_A, "UTC")));
        org.mockito.Mockito.when(tenantContext.currentTenantId()).thenReturn(Optional.of(TENANT_A));
        txTemplate = new TransactionTemplate(transactionManager);

        txTemplate.execute(status -> {
            List<DeliveryZoneCoordinate> coords = List.of(
                    new DeliveryZoneCoordinate(10.0, 10.0), new DeliveryZoneCoordinate(10.0, 11.0),
                    new DeliveryZoneCoordinate(11.0, 11.0), new DeliveryZoneCoordinate(10.0, 10.0)
            );
            com.transportlogistics.app.delivery.domain.model.DeliveryZoneBoundary boundary =
                    new com.transportlogistics.app.delivery.domain.model.DeliveryZoneBoundary(coords);
            DeliveryZone zoneA = new DeliveryZone(
                    UUID.randomUUID(), TENANT_A, "ZONE-RDR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                    "Rider Test Zone", "Desc",
                    com.transportlogistics.app.delivery.domain.model.DeliveryZoneType.URBAN_DENSE,
                    com.transportlogistics.app.delivery.domain.model.DeliveryZoneStatus.ACTIVE,
                    true, 20, null, boundary, 1, 0L, now, "test", now, "test"
            );
            zoneAId = zoneAdapter.save(zoneA).id();
            return null;
        });
    }

    @Test
    @DisplayName("Should persist and retrieve DeliveryRider with primary and secondary zones under tenant isolation")
    void persistAndRetrieveRider() {
        UUID driverId = UUID.randomUUID();
        String code = "RDR-PERSIST-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        txTemplate.execute(status -> {
            DeliveryRider rider = DeliveryRider.create(
                    UUID.randomUUID(), TENANT_A, code, driverId,
                    DeliveryRiderType.FULL_TIME, DeliveryTransportMode.BICYCLE, zoneAId, Set.of(), 5, "actor", now
            );
            riderAdapter.save(rider);
            return null;
        });

        // Tenant A sees the rider
        Optional<DeliveryRider> retrievedA = riderAdapter.findByRiderCode(code, TENANT_A);
        assertThat(retrievedA).isPresent();
        assertThat(retrievedA.get().getPrimaryZoneId()).isEqualTo(zoneAId);

        // Tenant B cannot see Tenant A's rider
        Optional<DeliveryRider> retrievedB = riderAdapter.findByRiderCode(code, TENANT_B);
        assertThat(retrievedB).isEmpty();
    }

    @Test
    @DisplayName("Should enforce partial unique constraint uk_active_driver_rider for active driver rider profiles")
    void duplicateActiveDriverRiderRejected() {
        UUID driverId = UUID.randomUUID();
        String code1 = "RDR-D1-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String code2 = "RDR-D2-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        txTemplate.execute(status -> {
            DeliveryRider rider1 = DeliveryRider.create(
                    UUID.randomUUID(), TENANT_A, code1, driverId,
                    DeliveryRiderType.FULL_TIME, DeliveryTransportMode.VAN, zoneAId, Set.of(), 5, "actor", now
            );
            riderAdapter.save(rider1);
            return null;
        });

        assertThatThrownBy(() -> txTemplate.execute(status -> {
            DeliveryRider rider2 = DeliveryRider.create(
                    UUID.randomUUID(), TENANT_A, code2, driverId,
                    DeliveryRiderType.GIG, DeliveryTransportMode.MOTORBIKE, zoneAId, Set.of(), 5, "actor", now
            );
            riderAdapter.save(rider2);
            return null;
        })).isNotNull();
    }

    @Test
    @DisplayName("Should manage shifts and enforce uk_active_delivery_order_rider unique index")
    void manageShiftsAndOrderAssignments() {
        UUID driverId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        txTemplate.execute(status -> {
            DeliveryRider rider = DeliveryRider.create(
                    riderId, TENANT_A, "RDR-ASSIGN-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(), driverId,
                    DeliveryRiderType.FULL_TIME, DeliveryTransportMode.MOTORBIKE, zoneAId, Set.of(), 5, "actor", now
            );
            riderAdapter.save(rider);

            DeliveryRiderShift shift = DeliveryRiderShift.create(
                    UUID.randomUUID(), TENANT_A, riderId, LocalDate.of(2026, 9, 1),
                    LocalTime.of(8, 0), LocalTime.of(16, 0), null, 5, "actor", now
            );
            riderAdapter.saveShift(shift);

            int r = (int) (Math.random() * 900000) + 100000;
            DeliveryOrder order = new DeliveryOrder(
                    new DeliveryId(orderId), new DeliveryNumber("DEL-2026-" + r), UUID.randomUUID(),
                    UUID.randomUUID(), UUID.randomUUID(), DeliveryPriority.NORMAL, DeliveryServiceType.STANDARD,
                    new DeliveryWindow(now, now.plusHours(2)), "Leave at gate", DeliveryStatus.READY_FOR_ASSIGNMENT,
                    0L, now, now, "system", "system"
            );
            orderAdapter.save(order);

            DeliveryOrderRiderAssignment assignment = DeliveryOrderRiderAssignment.create(
                    UUID.randomUUID(), TENANT_A, orderId, riderId, false, null, "dispatcher", now
            );
            riderAdapter.saveAssignment(assignment);
            return null;
        });

        // Query active assignments
        int count = riderAdapter.countActiveAssignmentsForRider(riderId, TENANT_A);
        assertThat(count).isEqualTo(1);

        Optional<DeliveryOrderRiderAssignment> active = riderAdapter.findActiveAssignmentForOrder(orderId, TENANT_A);
        assertThat(active).isPresent();
        assertThat(active.get().getRiderId()).isEqualTo(riderId);

        // Attempting another ACTIVE assignment for the same order should violate uk_active_delivery_order_rider
        assertThatThrownBy(() -> txTemplate.execute(status -> {
            DeliveryOrderRiderAssignment dup = DeliveryOrderRiderAssignment.create(
                    UUID.randomUUID(), TENANT_A, orderId, UUID.randomUUID(), false, null, "dispatcher", now
            );
            riderAdapter.saveAssignment(dup);
            return null;
        })).isNotNull();
    }
}
