package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.Coverage;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.OverlayType;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.SelectionType;
import com.transportlogistics.app.tracking.ports.outbound.JourneyReplayAuditPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
final class JdbcJourneyReplayAuditAdapter implements JourneyReplayAuditPort {
    private final JdbcTemplate jdbc;

    JdbcJourneyReplayAuditAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void record(UUID tenantId, UUID actorId, String correlationId, String action,
            SelectionType selectorType, UUID selectorId, Duration duration, int resultCount,
            Coverage coverage, Set<OverlayType> overlays, Instant occurredAt) {
        String detail = "correlationId=" + safe(correlationId) + ";selectorType=" + selectorType
                + ";selectorHash=" + hash(tenantId + ":" + selectorId)
                + ";durationSeconds=" + duration.toSeconds() + ";resultCount=" + resultCount
                + ";coverage=" + coverage + ";overlays=" + overlays.stream().sorted().toList();
        jdbc.update("INSERT INTO tracking_audit_event(id,tenant_id,actor_id,action,target_type,target_id,safe_detail,occurred_at) VALUES(?,?,?,?,?,?,?,?)",
                UUID.randomUUID(), tenantId, actorId, action, "JOURNEY_REPLAY",
                UUID.nameUUIDFromBytes((tenantId + ":" + selectorId).getBytes(StandardCharsets.UTF_8)),
                detail, java.sql.Timestamp.from(occurredAt));
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String safe(String value) { return value == null ? "" : value.replaceAll("[^A-Za-z0-9_-]", ""); }
}
