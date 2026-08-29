package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.domain.model.*;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryOrderUseCase;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryOrderRepository;
import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.tenancy.CanonicalTenant;
import com.transportlogistics.app.tenancy.TenantContextExecutor;
import com.transportlogistics.app.tenancy.TenantExecutionContext;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryOrderPersistencePostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {

    private static final UUID TENANT_B = UUID.fromString("7c3e44b7-68dc-4bcb-a53c-f1a8d5df0da2");
    private static final TenantExecutionContext TENANT_A_CONTEXT = context(CanonicalTenant.ID, "tenant-a");
    private static final TenantExecutionContext TENANT_B_CONTEXT = context(TENANT_B, "tenant-b");

    @Autowired private Flyway flyway;
    @Autowired private DeliveryOrderRepository orders;
    @Autowired private TenantContextExecutor contexts;

    @BeforeEach
    void resetDatabase() {
        flyway.clean();
        flyway.migrate();
    }

    @Test
    void isolatesDirectIdNumberSearchAndCountsBetweenTenants() {
        var orderA = order("DEL-2026-000001", "tenant-a");
        var orderB = order("DEL-2026-000001", "tenant-b");

        contexts.within(TENANT_A_CONTEXT, () -> orders.save(orderA));
        contexts.within(TENANT_B_CONTEXT, () -> orders.save(orderB));

        assertTenantCanSeeOnly(TENANT_A_CONTEXT, orderA, orderB);
        assertTenantCanSeeOnly(TENANT_B_CONTEXT, orderB, orderA);
    }

    private void assertTenantCanSeeOnly(TenantExecutionContext context, DeliveryOrder visible, DeliveryOrder hidden) {
        contexts.within(context, () -> {
            assertThat(orders.findById(visible.id().value())).isPresent();
            assertThat(orders.findById(hidden.id().value())).isEmpty();
            assertThat(orders.findByDeliveryNumber("DEL-2026-000001"))
                    .map(order -> order.id().value()).contains(visible.id().value());

            var result = orders.search(new DeliveryOrderUseCase.SearchQuery(
                    null, null, null, null, null, 0, 20));
            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.content()).extracting(order -> order.id().value())
                    .containsExactly(visible.id().value());
        });
    }

    private static DeliveryOrder order(String number, String actor) {
        var now = OffsetDateTime.parse("2026-08-29T10:00:00Z");
        return DeliveryOrder.create(new DeliveryId(UUID.randomUUID()), new DeliveryNumber(number), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), DeliveryPriority.NORMAL, DeliveryServiceType.STANDARD,
                new DeliveryWindow(now.plusDays(1), now.plusDays(2)), "Acceptance test", now, actor);
    }

    private static TenantExecutionContext context(UUID tenantId, String username) {
        return new TenantExecutionContext(tenantId, UUID.randomUUID(), username, "us56-acceptance");
    }
}
