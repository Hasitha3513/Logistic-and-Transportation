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
        try (var conn = java.sql.DriverManager.getConnection(
                configuredJdbcUrl(), configuredDatabaseUsername(), configuredDatabasePassword())) {
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static final UUID TENANT_B = UUID.fromString("7c3e44b7-68dc-4bcb-a53c-f1a8d5df0da2");
    private static final TenantExecutionContext TENANT_A_CONTEXT = context(CanonicalTenant.ID, "tenant-a");
    private static final TenantExecutionContext TENANT_B_CONTEXT = context(TENANT_B, "tenant-b");

    @Autowired private Flyway flyway;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired private DeliveryOrderRepository orders;
    @Autowired private DeliveryAttemptRepository attempts;
    @Autowired private DeliveryRedeliveryScheduleRepository schedules;
    @Autowired private TenantContextExecutor contexts;
    @Autowired private com.transportlogistics.app.delivery.ports.inbound.RedeliveryUseCase redeliveryService;

    @BeforeEach
    void resetDatabase() {
        flyway.clean();
        flyway.migrate();
        jdbc.update("""
                INSERT INTO tenant (tenant_id, tenant_code, tenant_name, default_currency, default_time_zone, status, created_at, created_by, updated_at, updated_by, version)
                VALUES (?, 'tenant-b', 'Tenant B', 'LKR', 'Asia/Colombo', 'ACTIVE', NOW(), 'system', NOW(), 'system', 0)
                ON CONFLICT (tenant_id) DO NOTHING
                """, TENANT_B);
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

    @Test
    void halfOpenIntervalOverlapDoesNotFalselyOverlapAdjacentWindows() {
        OffsetDateTime base = OffsetDateTime.parse("2026-09-01T09:00:00+05:30");
        OffsetDateTime win1Start = base;
        OffsetDateTime win1End = base.plusHours(1); // 09:00 - 10:00
        OffsetDateTime win2Start = base.plusHours(1); // 10:00 - 11:00
        OffsetDateTime win2End = base.plusHours(2);

        contexts.within(TENANT_A_CONTEXT, () -> {
            UUID orderId = UUID.randomUUID();
            UUID attemptId = UUID.randomUUID();
            UUID scheduleId = UUID.randomUUID();

            orders.save(new DeliveryOrder(
                    new DeliveryId(orderId), new DeliveryNumber("DEL-2026-000101"),
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    DeliveryPriority.NORMAL, DeliveryServiceType.STANDARD,
                    new DeliveryWindow(win1Start, win1End), null,
                    DeliveryStatus.FAILED_ATTEMPT, 1L, base, base, "creator", "creator"
            ));
            attempts.save(DeliveryAttempt.create(
                    attemptId, new DeliveryId(orderId), 1, base,
                    DeliveryFailureReason.CUSTOMER_UNAVAILABLE, "Not home",
                    DeliveryFailureDisposition.REDELIVERY_ELIGIBLE, null, "operator", base
            ));
            schedules.save(DeliveryRedeliverySchedule.createConfirmed(
                    scheduleId, CanonicalTenant.ID, new DeliveryId(orderId), attemptId,
                    RedeliverySchedulingMethod.AUTOMATIC, null, null, null,
                    win1Start, win1End, "operator", base
            ));

            // Querying for 10:00 - 11:00 must NOT overlap with 09:00 - 10:00
            int overlap = schedules.countActiveOverlapping(CanonicalTenant.ID, win2Start, win2End, null);
            assertThat(overlap).isEqualTo(0);

            // Querying for 09:30 - 10:30 MUST overlap
            int partialOverlap = schedules.countActiveOverlapping(CanonicalTenant.ID, base.plusMinutes(30), base.plusMinutes(90), null);
            assertThat(partialOverlap).isEqualTo(1);
        });
    }

    @Test
    void capacityLimitEnforcesMaxFiftyConcurrentDeliveriesPerWindow() {
        OffsetDateTime base = OffsetDateTime.now().plusDays(1)
                .withHour(10).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime windowEnd = base.plusHours(2);

        contexts.within(TENANT_A_CONTEXT, () -> {
            // Seed 49 schedules in this window
            for (int i = 1; i <= 49; i++) {
                UUID orderId = UUID.randomUUID();
                UUID attemptId = UUID.randomUUID();
                orders.save(new DeliveryOrder(
                        new DeliveryId(orderId), new DeliveryNumber("DEL-2026-" + String.format("%06d", i)),
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        DeliveryPriority.NORMAL, DeliveryServiceType.STANDARD,
                        new DeliveryWindow(base, windowEnd), null,
                        DeliveryStatus.FAILED_ATTEMPT, 1L, base, base, "creator", "creator"
                ));
                attempts.save(DeliveryAttempt.create(
                        attemptId, new DeliveryId(orderId), 1, base,
                        DeliveryFailureReason.CUSTOMER_UNAVAILABLE, "Not home",
                        DeliveryFailureDisposition.REDELIVERY_ELIGIBLE, null, "operator", base
                ));
                schedules.save(DeliveryRedeliverySchedule.createConfirmed(
                        UUID.randomUUID(), CanonicalTenant.ID, new DeliveryId(orderId), attemptId,
                        RedeliverySchedulingMethod.AUTOMATIC, null, null, null,
                        base, windowEnd, "seeder", base
                ));
            }

            int count49 = schedules.countActiveOverlapping(CanonicalTenant.ID, base, windowEnd, null);
            assertThat(count49).isEqualTo(49);

            // Create 50th order and schedule
            UUID order50Id = UUID.randomUUID();
            UUID attempt50Id = UUID.randomUUID();
            orders.save(new DeliveryOrder(
                    new DeliveryId(order50Id), new DeliveryNumber("DEL-2026-000050"),
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    DeliveryPriority.NORMAL, DeliveryServiceType.STANDARD,
                    new DeliveryWindow(base, windowEnd), null,
                    DeliveryStatus.FAILED_ATTEMPT, 1L, base, base, "creator", "creator"
            ));
            attempts.save(DeliveryAttempt.create(
                    attempt50Id, new DeliveryId(order50Id), 1, base,
                    DeliveryFailureReason.CUSTOMER_UNAVAILABLE, "Not home",
                    DeliveryFailureDisposition.REDELIVERY_ELIGIBLE, null, "operator", base
            ));

            var schedule50 = redeliveryService.scheduleRedelivery(order50Id, new com.transportlogistics.app.delivery.ports.inbound.RedeliveryUseCase.ScheduleRedeliveryCommand(
                    1L, attempt50Id, RedeliverySchedulingMethod.AGENT_ASSISTED,
                    null, null, null, base, windowEnd
            ), "dispatcher");
            assertThat(schedule50.status()).isEqualTo(RedeliveryScheduleStatus.CONFIRMED);

            // Now at 50/50 capacity
            int count50 = schedules.countActiveOverlapping(CanonicalTenant.ID, base, windowEnd, null);
            assertThat(count50).isEqualTo(50);

            // Create 51st order - scheduling into this same window MUST fail with capacity exceeded
            UUID order51Id = UUID.randomUUID();
            UUID attempt51Id = UUID.randomUUID();
            orders.save(new DeliveryOrder(
                    new DeliveryId(order51Id), new DeliveryNumber("DEL-2026-000051"),
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    DeliveryPriority.NORMAL, DeliveryServiceType.STANDARD,
                    new DeliveryWindow(base, windowEnd), null,
                    DeliveryStatus.FAILED_ATTEMPT, 1L, base, base, "creator", "creator"
            ));
            attempts.save(DeliveryAttempt.create(
                    attempt51Id, new DeliveryId(order51Id), 1, base,
                    DeliveryFailureReason.CUSTOMER_UNAVAILABLE, "Not home",
                    DeliveryFailureDisposition.REDELIVERY_ELIGIBLE, null, "operator", base
            ));

            org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                    redeliveryService.scheduleRedelivery(order51Id, new com.transportlogistics.app.delivery.ports.inbound.RedeliveryUseCase.ScheduleRedeliveryCommand(
                            1L, attempt51Id, RedeliverySchedulingMethod.AGENT_ASSISTED,
                            null, null, null, base, windowEnd
                    ), "dispatcher")
            ).isInstanceOf(com.transportlogistics.app.shared.domain.ConflictException.class)
                    .hasMessageContaining("capacity exceeded");
        });
    }

    private static TenantExecutionContext context(UUID tenantId, String username) {
        return new TenantExecutionContext(tenantId, UUID.randomUUID(), username, "us60-acceptance");
    }
}
