package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.domain.model.*;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryAttemptRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryOrderRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryRedeliveryScheduleRepository;
import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.tenancy.CanonicalTenant;
import com.transportlogistics.app.tenancy.TenantContextExecutor;
import com.transportlogistics.app.tenancy.TenantExecutionContext;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("postgres")
@EnabledIf("postgresAvailable")
class RedeliveryPersistencePostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {

    private static boolean postgresAvailable() {
        if (POSTGRES != null && POSTGRES.isRunning()) {
            return true;
        }
        String url = System.getProperty("DB_URL", "jdbc:postgresql://localhost:5432/transport_integration");
        String user = System.getProperty("DB_USERNAME", "transport_app");
        String pass = System.getProperty("DB_PASSWORD", "LocalDb-Transport-2026");
        try (var conn = java.sql.DriverManager.getConnection(url, user, pass)) {
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static final UUID TENANT_B = UUID.fromString("7c3e44b7-68dc-4bcb-a53c-f1a8d5df0da2");
    private static final TenantExecutionContext TENANT_A_CONTEXT = context(CanonicalTenant.ID, "tenant-a");
    private static final TenantExecutionContext TENANT_B_CONTEXT = context(TENANT_B, "tenant-b");

    @Autowired private Flyway flyway;
    @Autowired private DeliveryOrderRepository orders;
    @Autowired private DeliveryAttemptRepository attempts;
    @Autowired private DeliveryRedeliveryScheduleRepository schedules;
    @Autowired private TenantContextExecutor contexts;

    @BeforeEach
    void resetDatabase() {
        flyway.clean();
        flyway.migrate();
    }

    @Test
    void savesAndIsolatesRedeliverySchedulesAcrossTenants() {
        UUID orderIdA = UUID.randomUUID();
        UUID orderIdB = UUID.randomUUID();
        UUID attemptIdA = UUID.randomUUID();
        UUID attemptIdB = UUID.randomUUID();
        UUID scheduleIdA = UUID.randomUUID();
        UUID scheduleIdB = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        DeliveryOrder orderA = new DeliveryOrder(
                new DeliveryId(orderIdA), new DeliveryNumber("DEL-2026-000001"),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                DeliveryPriority.NORMAL, DeliveryServiceType.STANDARD,
                new DeliveryWindow(now, now.plusHours(4)), null,
                DeliveryStatus.FAILED_ATTEMPT, 1L, now, now, "creator", "creator"
        );
        DeliveryOrder orderB = new DeliveryOrder(
                new DeliveryId(orderIdB), new DeliveryNumber("DEL-2026-000001"),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                DeliveryPriority.NORMAL, DeliveryServiceType.STANDARD,
                new DeliveryWindow(now, now.plusHours(4)), null,
                DeliveryStatus.FAILED_ATTEMPT, 1L, now, now, "creator", "creator"
        );

        DeliveryAttempt attemptA = DeliveryAttempt.create(
                attemptIdA, new DeliveryId(orderIdA), 1, now,
                DeliveryFailureReason.CUSTOMER_UNAVAILABLE, "Not available",
                DeliveryFailureDisposition.REDELIVERY_ELIGIBLE, null, "operator", now
        );
        DeliveryAttempt attemptB = DeliveryAttempt.create(
                attemptIdB, new DeliveryId(orderIdB), 1, now,
                DeliveryFailureReason.CUSTOMER_UNAVAILABLE, "Not available",
                DeliveryFailureDisposition.REDELIVERY_ELIGIBLE, null, "operator", now
        );

        DeliveryRedeliverySchedule schedA = DeliveryRedeliverySchedule.createConfirmed(
                scheduleIdA, CanonicalTenant.ID, new DeliveryId(orderIdA), attemptIdA,
                RedeliverySchedulingMethod.AGENT_ASSISTED, null, null, null,
                now.plusDays(1), now.plusDays(1).plusHours(4), "dispatcher", now
        );
        DeliveryRedeliverySchedule schedB = DeliveryRedeliverySchedule.createConfirmed(
                scheduleIdB, TENANT_B, new DeliveryId(orderIdB), attemptIdB,
                RedeliverySchedulingMethod.AGENT_ASSISTED, null, null, null,
                now.plusDays(1), now.plusDays(1).plusHours(4), "dispatcher", now
        );

        contexts.within(TENANT_A_CONTEXT, () -> {
            orders.save(orderA);
            attempts.save(attemptA);
            schedules.save(schedA);
        });

        contexts.within(TENANT_B_CONTEXT, () -> {
            orders.save(orderB);
            attempts.save(attemptB);
            schedules.save(schedB);
        });

        contexts.within(TENANT_A_CONTEXT, () -> {
            var found = schedules.findById(scheduleIdA);
            assertThat(found).isPresent();
            assertThat(found.get().tenantId()).isEqualTo(CanonicalTenant.ID);

            var crossTenant = schedules.findById(scheduleIdB);
            assertThat(crossTenant).isEmpty();

            var history = schedules.findByDeliveryOrderId(orderIdA);
            assertThat(history).hasSize(1);

            int overlap = schedules.countActiveOverlapping(CanonicalTenant.ID, now.plusDays(1), now.plusDays(1).plusHours(4), null);
            assertThat(overlap).isEqualTo(1);
        });
    }

    private static TenantExecutionContext context(UUID tenantId, String username) {
        return new TenantExecutionContext(tenantId, UUID.randomUUID(), username, "us60-acceptance");
    }
}
