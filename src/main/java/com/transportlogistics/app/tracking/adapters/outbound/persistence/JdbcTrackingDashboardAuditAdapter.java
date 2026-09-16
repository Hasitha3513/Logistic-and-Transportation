package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import com.transportlogistics.app.tracking.ports.outbound.TrackingDashboardAuditPort;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
final class JdbcTrackingDashboardAuditAdapter implements TrackingDashboardAuditPort {
    private final JdbcTemplate jdbc;

    JdbcTrackingDashboardAuditAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void record(UUID tenantId, UUID actorId, String correlationId, String action,
            Set<String> filterCategories, int requestedPageSize, int resultCount,
            Set<String> includedSections, Set<String> sourceStatuses, Instant occurredAt) {
        String detail = detail(correlationId, filterCategories, requestedPageSize, resultCount,
                includedSections, sourceStatuses);
        jdbc.update("INSERT INTO tracking_audit_event(id,tenant_id,actor_id,action,target_type,target_id,safe_detail,occurred_at) VALUES(?,?,?,?,?,?,?,?)",
                UUID.randomUUID(), tenantId, actorId, action, "TRACKING_DASHBOARD", tenantId,
                detail, Timestamp.from(occurredAt));
    }

    static String detail(String correlationId, Set<String> filterCategories, int requestedPageSize,
            int resultCount, Set<String> includedSections, Set<String> sourceStatuses) {
        return "c=" + safe(correlationId)
                + ";f=" + joined(filterCategories)
                + ";p=" + requestedPageSize + ";r=" + resultCount
                + ";i=" + joined(includedSections)
                + ";s=" + joined(sourceStatuses);
    }

    private static String safe(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9_-]", "");
    }

    private static String joined(Set<String> values) {
        return String.join(",", values.stream().sorted().toList());
    }
}
