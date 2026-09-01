package com.transportlogistics.app.delivery;

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
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderType;
import com.transportlogistics.app.delivery.domain.model.DeliveryTransportMode;
import com.transportlogistics.app.delivery.domain.model.DeliveryServiceType;
import com.transportlogistics.app.delivery.domain.model.DeliveryStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryWindow;
import com.transportlogistics.app.delivery.domain.model.DeliveryZone;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneCoordinate;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryBatchRepository;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class DeliveryBatchPostgreSqlAcceptanceTest {

    private static final UUID TENANT_A = com.transportlogistics.app.tenancy.CanonicalTenant.ID;
    private static final UUID TENANT_B = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Autowired
    private DeliveryBatchRepository batchAdapter;

    @Autowired
    private DeliveryRiderRepository riderAdapter;

    @Autowired
    private DeliveryZoneRepository zoneAdapter;

    @Autowired
    private DeliveryOrderRepository orderAdapter;

    @Autowired
    private com.transportlogistics.app.tenancy.TenantContextExecutor tenantExecutor;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort tenantContext;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate txTemplate;
    private final OffsetDateTime now = OffsetDateTime.now();

    @BeforeEach
    void setUp() {
        txTemplate = new TransactionTemplate(transactionManager);
        org.mockito.Mockito.when(tenantContext.currentTenantId()).thenReturn(Optional.of(TENANT_A));
    }

    private DeliveryZone seedZone(UUID tenantId, String code) {
        List<DeliveryZoneCoordinate> coords = List.of(
                new DeliveryZoneCoordinate(10.0, 10.0),
                new DeliveryZoneCoordinate(10.0, 20.0),
                new DeliveryZoneCoordinate(20.0, 20.0),
                new DeliveryZoneCoordinate(20.0, 10.0),
                new DeliveryZoneCoordinate(10.0, 10.0)
        );
        com.transportlogistics.app.delivery.domain.model.DeliveryZoneBoundary boundary =
                new com.transportlogistics.app.delivery.domain.model.DeliveryZoneBoundary(coords);
        return zoneAdapter.save(new DeliveryZone(
                UUID.randomUUID(), tenantId, code, "Zone " + code, "Acceptance Zone",
                com.transportlogistics.app.delivery.domain.model.DeliveryZoneType.URBAN_DENSE,
                com.transportlogistics.app.delivery.domain.model.DeliveryZoneStatus.ACTIVE,
                true, 100, null, boundary, 0, 0L, now, "admin", now, "admin"
        ));
    }

    private DeliveryOrder seedOrder(UUID tenantId, String prefix) {
        int r = (int) (Math.random() * 900000) + 100000;
        var ctx = new com.transportlogistics.app.tenancy.TenantExecutionContext(tenantId, UUID.randomUUID(), "test-user", "corr-id");
        return tenantExecutor.within(ctx, () -> orderAdapter.save(new DeliveryOrder(
                new DeliveryId(UUID.randomUUID()),
                new DeliveryNumber("DEL-2026-" + r),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                DeliveryPriority.NORMAL,
                DeliveryServiceType.STANDARD,
                new DeliveryWindow(now, now.plusHours(2)),
                "Fragile",
                DeliveryStatus.READY_FOR_ASSIGNMENT,
                0L,
                now,
                now,
                "admin",
                "admin"
        )));
    }

    private DeliveryBatchCode uniqueBatchCode() {
        int r = (int) (Math.random() * 900000) + 100000;
        return new DeliveryBatchCode("BAT-2026-" + r);
    }

    private DeliveryRider seedRider(UUID tenantId, String code, UUID zoneId) {
        return riderAdapter.save(DeliveryRider.create(
                UUID.randomUUID(), tenantId, code, UUID.randomUUID(), DeliveryRiderType.FULL_TIME,
                DeliveryTransportMode.MOTORBIKE, zoneId, Set.of(), 5, "admin", now
        ));
    }

    @Test
    @DisplayName("Gate 1: PostgreSQL persistence of DeliveryBatch and DeliveryBatchOrder")
    void batchPersistence_succeeds() {
        txTemplate.execute(status -> {
            DeliveryZone zone = seedZone(TENANT_A, "BAT-Z1-" + UUID.randomUUID().toString().substring(0, 6));
            DeliveryOrder order = seedOrder(TENANT_A, "DEL-2026-900001");

            DeliveryBatch batch = DeliveryBatch.create(
                    UUID.randomUUID(), TENANT_A, uniqueBatchCode(),
                    zone.id(), null, 5, now, "admin"
            );
            DeliveryBatch savedBatch = batchAdapter.save(batch);

            assertThat(savedBatch).isNotNull();
            assertThat(savedBatch.status()).isEqualTo(DeliveryBatchStatus.DRAFT);

            DeliveryBatchOrder batchOrder = DeliveryBatchOrder.create(
                    UUID.randomUUID(), TENANT_A, savedBatch.id(), order.id().value(), 1, now, "admin"
            );
            DeliveryBatchOrder savedOrder = batchAdapter.saveOrderMembership(batchOrder);

            assertThat(savedOrder).isNotNull();
            assertThat(savedOrder.status()).isEqualTo(DeliveryBatchOrderStatus.ACTIVE);

            List<DeliveryBatchOrder> activeMembers = batchAdapter.findActiveOrderMembershipsByBatchId(TENANT_A, savedBatch.id());
            assertThat(activeMembers).hasSize(1);
            assertThat(activeMembers.get(0).deliveryOrderId()).isEqualTo(order.id().value());

            return null;
        });
    }

    @Test
    @DisplayName("Gate 2: Active membership uniqueness constraint (uk_active_batch_order)")
    void activeMembershipUniqueness_enforced() {
        DeliveryZone zone = seedZone(TENANT_A, "BAT-Z2-" + UUID.randomUUID().toString().substring(0, 6));
        DeliveryOrder order = seedOrder(TENANT_A, "DEL-2026-900002");

        DeliveryBatch batch1 = batchAdapter.save(DeliveryBatch.create(
                UUID.randomUUID(), TENANT_A, uniqueBatchCode(), zone.id(), null, 5, now, "admin"
        ));
        DeliveryBatch batch2 = batchAdapter.save(DeliveryBatch.create(
                UUID.randomUUID(), TENANT_A, uniqueBatchCode(), zone.id(), null, 5, now, "admin"
        ));

        batchAdapter.saveOrderMembership(DeliveryBatchOrder.create(
                UUID.randomUUID(), TENANT_A, batch1.id(), order.id().value(), 1, now, "admin"
        ));

        assertThatThrownBy(() -> {
            txTemplate.execute(status -> {
                batchAdapter.saveOrderMembership(DeliveryBatchOrder.create(
                        UUID.randomUUID(), TENANT_A, batch2.id(), order.id().value(), 1, now, "admin"
                ));
                return null;
            });
        }).hasMessageContaining("uk_active_batch_order");
    }

    @Test
    @DisplayName("Gate 3: Same-Tenant foreign key constraint on delivery_batch_order")
    void sameTenantConstraint_enforced() {
        DeliveryZone zone = seedZone(TENANT_A, "BAT-Z3-" + UUID.randomUUID().toString().substring(0, 6));
        DeliveryBatch batch = batchAdapter.save(DeliveryBatch.create(
                UUID.randomUUID(), TENANT_A, uniqueBatchCode(), zone.id(), null, 5, now, "admin"
        ));

        // Create Tenant B order under TENANT_B context
        org.mockito.Mockito.when(tenantContext.currentTenantId()).thenReturn(Optional.of(TENANT_B));
        org.mockito.Mockito.when(tenantContext.currentTenant()).thenReturn(Optional.of(new com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort.TenantContext(TENANT_B, "UTC")));
        DeliveryOrder orderTenantB = seedOrder(TENANT_B, "DEL-2026-900003");

        // Switch back to TENANT_A
        org.mockito.Mockito.when(tenantContext.currentTenantId()).thenReturn(Optional.of(TENANT_A));
        org.mockito.Mockito.when(tenantContext.currentTenant()).thenReturn(Optional.of(new com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort.TenantContext(TENANT_A, "UTC")));

        assertThatThrownBy(() -> {
            txTemplate.execute(status -> {
                batchAdapter.saveOrderMembership(DeliveryBatchOrder.create(
                        UUID.randomUUID(), TENANT_A, batch.id(), orderTenantB.id().value(), 1, now, "admin"
                ));
                return null;
            });
        }).hasMessageContaining("fk_delivery_batch_order_order_tenant");
    }

    @Test
    @DisplayName("Gate 4: Batch lifecycle updates and rider assignment persistence")
    void batchLifecycleAndRider_persists() {
        txTemplate.execute(status -> {
            DeliveryZone zone = seedZone(TENANT_A, "BAT-Z4-" + UUID.randomUUID().toString().substring(0, 6));
            DeliveryRider rider = seedRider(TENANT_A, "RDR-B1-" + UUID.randomUUID().toString().substring(0, 6), zone.id());

            DeliveryBatch batch = batchAdapter.save(DeliveryBatch.create(
                    UUID.randomUUID(), TENANT_A, uniqueBatchCode(), zone.id(), null, 5, now, "admin"
            ));

            DeliveryBatch assigned = batch.assignRider(rider.getId(), now, "admin");
            batchAdapter.save(assigned);

            Optional<DeliveryBatch> reloaded = batchAdapter.findById(TENANT_A, batch.id());
            assertThat(reloaded).isPresent();
            assertThat(reloaded.get().status()).isEqualTo(DeliveryBatchStatus.ASSIGNED);
            assertThat(reloaded.get().riderId()).isEqualTo(rider.getId());

            DeliveryBatch dispatched = reloaded.get().dispatch(now, "admin");
            batchAdapter.save(dispatched);

            Optional<DeliveryBatch> dispatchedReloaded = batchAdapter.findById(TENANT_A, batch.id());
            assertThat(dispatchedReloaded).isPresent();
            assertThat(dispatchedReloaded.get().status()).isEqualTo(DeliveryBatchStatus.DISPATCHED);

            return null;
        });
    }

    @Test
    @DisplayName("Gate 5: Multi-Tenant isolation - Tenant B cannot view Tenant A batch")
    void tenantIsolation_enforced() {
        DeliveryZone zone = seedZone(TENANT_A, "BAT-Z5-" + UUID.randomUUID().toString().substring(0, 6));
        DeliveryBatch batchTenantA = batchAdapter.save(DeliveryBatch.create(
                UUID.randomUUID(), TENANT_A, uniqueBatchCode(), zone.id(), null, 5, now, "admin"
        ));

        Optional<DeliveryBatch> tenantBAccess = batchAdapter.findById(TENANT_B, batchTenantA.id());
        assertThat(tenantBAccess).isEmpty();
    }
}
