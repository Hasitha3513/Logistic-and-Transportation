package com.transportlogistics.app.operations.adapters.inbound.web.controllers;

import com.transportlogistics.app.operations.OperationalExceptionFactV1;
import com.transportlogistics.app.shared.DurableEventPublisher;
import com.transportlogistics.app.shared.DurableEventWorker;
import com.transportlogistics.app.tenancy.CurrentTenant;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/** Profile-restricted deterministic controls for the isolated real E2E runtime. */
@RestController
@Profile("e2e")
@RequestMapping("/e2e/operational-exceptions")
public class E2eOperationalExceptionTestController {
    private final DurableEventPublisher events;
    private final DurableEventWorker worker;
    private final JdbcClient jdbc;
    private final CurrentTenant currentTenant;

    public E2eOperationalExceptionTestController(DurableEventPublisher events, DurableEventWorker worker,
                                                  JdbcClient jdbc, CurrentTenant currentTenant) {
        this.events = events;
        this.worker = worker;
        this.jdbc = jdbc;
        this.currentTenant = currentTenant;
    }

    @PostMapping("/process")
    void process() {
        worker.processDue();
    }

    @PostMapping("/{caseId}/replay")
    @Transactional
    void replay(@PathVariable UUID caseId) {
        UUID tenantId = currentTenant.required().tenantId();
        var row = jdbc.sql("""
                select source_event_id, source_module, source_type, source_id, occurred_at,
                       severity, category, summary_code, correlation_id
                  from operational_exception_case
                 where tenant_id = :tenantId and id = :caseId
                """).param("tenantId", tenantId).param("caseId", caseId).query((rs, ignored) -> new ReplayRow(
                    rs.getObject("source_event_id", UUID.class), rs.getString("source_module"),
                    rs.getString("source_type"), rs.getObject("source_id", UUID.class),
                    rs.getObject("occurred_at", OffsetDateTime.class), rs.getString("severity"),
                    rs.getString("category"), rs.getString("summary_code"), rs.getString("correlation_id")))
                .single();
        String sourceKey = "ROUTING".equals(row.sourceModule()) ? "routeId" : "deliveryOrderId";
        events.publish(new OperationalExceptionFactV1(row.eventId(), tenantId,
            OperationalExceptionFactV1.SourceModule.valueOf(row.sourceModule()), row.sourceType(), row.sourceId(),
            row.occurredAt(), OperationalExceptionFactV1.Severity.valueOf(row.severity()),
            OperationalExceptionFactV1.Category.valueOf(row.category()), row.summaryCode(),
            Map.of(sourceKey, row.sourceId().toString()), row.correlationId()));
    }

    @GetMapping("/{caseId}/evidence")
    Evidence evidence(@PathVariable UUID caseId) {
        UUID tenantId = currentTenant.required().tenantId();
        UUID sourceEventId = jdbc.sql("select source_event_id from operational_exception_case "
                + "where tenant_id=:tenantId and id=:caseId")
            .param("tenantId", tenantId).param("caseId", caseId).query(UUID.class).single();
        int caseCount = count("select count(*) from operational_exception_case "
            + "where tenant_id=:tenantId and source_event_id=:eventId", tenantId, sourceEventId);
        int intakeCount = count("select count(*) from integration_outbox_event "
            + "where tenant_id=:tenantId and event_id=:eventId and consumer_name='operations-exception-intake'",
            tenantId, sourceEventId);
        var notification = jdbc.sql("select status, payload::text from integration_outbox_event "
                + "where tenant_id=:tenantId and aggregate_id=:caseId and consumer_name='operations-notification' "
                + "order by created_at desc limit 1")
            .param("tenantId", tenantId).param("caseId", caseId)
            .query((rs, ignored) -> new NotificationEvidence(rs.getString(1), rs.getString(2))).optional();
        return new Evidence(caseCount, intakeCount, notification.orElse(null));
    }

    private int count(String sql, UUID tenantId, UUID id) {
        return jdbc.sql(sql).param("tenantId", tenantId).param(sql.contains("event_id") ? "eventId" : "caseId", id)
            .query(Integer.class).single();
    }

    private record ReplayRow(UUID eventId, String sourceModule, String sourceType, UUID sourceId,
                             OffsetDateTime occurredAt, String severity, String category,
                             String summaryCode, String correlationId) {}
    record Evidence(int caseCount, int intakeOutboxCount, NotificationEvidence notification) {}
    record NotificationEvidence(String status, String payload) {}
}
