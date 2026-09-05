package com.transportlogistics.app.operations.adapters.outbound.persistence;

import com.transportlogistics.app.operations.OperationalExceptionFactV1;
import com.transportlogistics.app.operations.domain.model.CorrectiveAction;
import com.transportlogistics.app.operations.domain.model.OperationalExceptionCase;
import com.transportlogistics.app.operations.domain.model.RootCauseAnalysis;
import com.transportlogistics.app.operations.ports.inbound.OperationalExceptionUseCase;
import com.transportlogistics.app.shared.DurableEventPublisher;
import com.transportlogistics.app.shared.DurableEventWorker;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.tenancy.TenantContextExecutor;
import com.transportlogistics.app.tenancy.TenantExecutionContext;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OperationalExceptionPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-04T01:00:00Z");
    @Autowired Flyway flyway;
    @Autowired JdbcTemplate jdbc;
    @Autowired DurableEventPublisher durableEvents;
    @Autowired DurableEventWorker durableWorker;
    @Autowired TenantContextExecutor tenantContexts;
    @Autowired TransactionTemplate transactions;
    @Autowired OperationalExceptionUseCase operations;

    @Test
    void v62OwnsFiveTenantConsistentTablesAndBoundedQueryIndexes() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("65");
        assertThat(jdbc.queryForList("select tablename from pg_tables where schemaname='public' "
            + "and tablename like 'operational_exception_%'", String.class)).containsExactlyInAnyOrder(
                "operational_exception_case", "operational_exception_assignment_history",
                "operational_exception_corrective_action", "operational_exception_rca",
                "operational_exception_history");
        var indexes = jdbc.queryForList("select indexdef from pg_indexes where schemaname='public' "
            + "and tablename like 'operational_exception_%'", String.class);
        assertThat(indexes).anyMatch(value -> value.contains("tenant_id, source_event_id"));
        assertThat(indexes).anyMatch(value -> value.contains("tenant_id, next_escalation_at, status"));
        assertThat(indexes).anyMatch(value -> value.contains("tenant_id, case_id, occurred_at DESC"));
    }

    @Test
    void durableReplayCreatesOneCaseAndLifecycleEnforcesTenantHistoryAndSod() {
        UUID tenant = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        UUID secondActor = UUID.randomUUID();
        var execution = new TenantExecutionContext(tenant, actor, "operations-author", "operations-test");
        var fact = new OperationalExceptionFactV1(UUID.randomUUID(), tenant,
            OperationalExceptionFactV1.SourceModule.DELIVERY, "DAMAGED_DELIVERY", UUID.randomUUID(), NOW,
            OperationalExceptionFactV1.Severity.HIGH, OperationalExceptionFactV1.Category.OPERATIONAL,
            "DELIVERY_EXCEPTION_CREATED", Map.of("deliveryOrderId", UUID.randomUUID().toString()), "operations-test");

        tenantContexts.within(execution, () -> {
            transactions.executeWithoutResult(status -> {
                durableEvents.publish(fact);
                durableEvents.publish(fact);
            });
            durableWorker.processDue();
        });

        assertThat(jdbc.queryForObject("select count(*) from operational_exception_case where tenant_id=? "
            + "and source_event_id=?", Integer.class, tenant, fact.eventId())).isEqualTo(1);
        UUID caseId = jdbc.queryForObject("select id from operational_exception_case where tenant_id=? "
            + "and source_event_id=?", UUID.class, tenant, fact.eventId());

        var context = new OperationalExceptionUseCase.Context(tenant, actor, "operations-author", "operations-test");
        tenantContexts.within(execution, () -> {
            var detail = operations.get(context, caseId);
            detail = operations.acknowledge(context, caseId,
                new OperationalExceptionUseCase.VersionCommand(detail.exceptionCase().version()));
            long acknowledgedVersion = detail.exceptionCase().version();
            assertThatThrownBy(() -> operations.start(context, caseId,
                new OperationalExceptionUseCase.VersionCommand(acknowledgedVersion - 1)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("reload and retry");
            detail = operations.start(context, caseId,
                new OperationalExceptionUseCase.VersionCommand(detail.exceptionCase().version()));
            detail = operations.addCorrectiveAction(context, caseId,
                new OperationalExceptionUseCase.CorrectiveActionCommand(detail.exceptionCase().version(),
                    CorrectiveAction.Type.CORRECTIVE, "Inspect and replace damaged packaging",
                    OperationalExceptionCase.AssignmentType.ROLE_QUEUE, null, "OPERATIONS_QUEUE", null, null));
            CorrectiveAction action = detail.correctiveActions().getFirst();
            detail = operations.completeCorrectiveAction(context, caseId, action.id(),
                new OperationalExceptionUseCase.VersionCommand(action.version()));
            detail = operations.recordRca(context, caseId,
                new OperationalExceptionUseCase.RcaCommand(detail.exceptionCase().version(),
                    RootCauseAnalysis.CauseCategory.PROCESS, "PACKAGING_CONTROL_GAP",
                    "Packaging inspection control was missed", "Dispatch checklist was incomplete"));
            detail = operations.approveRca(new OperationalExceptionUseCase.Context(tenant, secondActor,
                    "operations-approver", "operations-test"), caseId,
                new OperationalExceptionUseCase.RcaApprovalCommand(detail.exceptionCase().version(),
                    detail.rca().version()));
            detail = operations.resolve(context, caseId, new OperationalExceptionUseCase.ResolveCommand(
                detail.exceptionCase().version(), "Packaging replaced and source resolution recorded", "DELIVERY:RESOLVED"));
            detail = operations.close(new OperationalExceptionUseCase.Context(tenant, secondActor,
                    "operations-closer", "operations-test"), caseId,
                new OperationalExceptionUseCase.VersionCommand(detail.exceptionCase().version()));
            assertThat(detail.exceptionCase().status()).isEqualTo(OperationalExceptionCase.Status.CLOSED);
            assertThat(operations.history(context, caseId, 0, 50).content()).hasSizeGreaterThanOrEqualTo(8);
            assertThatThrownBy(() -> operations.get(new OperationalExceptionUseCase.Context(UUID.randomUUID(), actor,
                "tenant-b", "operations-test"), caseId)).isInstanceOf(NotFoundException.class);
        });

        assertThatThrownBy(() -> jdbc.update("insert into operational_exception_history "
            + "(id,tenant_id,case_id,action,actor_id,actor_username,resulting_version,occurred_at) "
            + "values (?,?,?,?,?,?,?,?)", UUID.randomUUID(), UUID.randomUUID(), caseId, "CROSS_TENANT",
            actor, "attacker", 0, NOW)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void slaWorkerEscalatesAnOverdueCaseAndAppendsAuditableHistory() {
        UUID tenant = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        var execution = new TenantExecutionContext(tenant, actor, "operations-sla-test", "operations-sla-test");
        var fact = new OperationalExceptionFactV1(UUID.randomUUID(), tenant,
            OperationalExceptionFactV1.SourceModule.ROUTING, "ROAD_CLOSURE", UUID.randomUUID(),
            OffsetDateTime.now().minusHours(12), OperationalExceptionFactV1.Severity.HIGH,
            OperationalExceptionFactV1.Category.OPERATIONAL, "ROUTE_DISRUPTION_CREATED",
            Map.of("routeId", UUID.randomUUID().toString()), "operations-sla-test");

        tenantContexts.within(execution, () -> {
            transactions.executeWithoutResult(status -> durableEvents.publish(fact));
            durableWorker.processDue();
        });
        UUID caseId = jdbc.queryForObject("select id from operational_exception_case where tenant_id=? "
            + "and source_event_id=?", UUID.class, tenant, fact.eventId());

        assertThat(tenantContexts.within(execution, () -> operations.scanDue(tenant))).isEqualTo(1);
        assertThat(jdbc.queryForObject("select escalation_level from operational_exception_case where tenant_id=? "
            + "and id=?", String.class, tenant, caseId)).isEqualTo("L1");
        assertThat(jdbc.queryForObject("select count(*) from operational_exception_history where tenant_id=? "
            + "and case_id=? and action='ESCALATED' and actor_username='system:operational-exception-sla'",
            Integer.class, tenant, caseId)).isEqualTo(1);
    }
}
