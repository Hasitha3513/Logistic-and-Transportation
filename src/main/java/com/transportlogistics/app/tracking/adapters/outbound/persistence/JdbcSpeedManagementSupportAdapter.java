package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.tracking.ports.outbound.SpeedManagementSupportPort;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class JdbcSpeedManagementSupportAdapter implements SpeedManagementSupportPort {
    private final JdbcTemplate jdbc;

    JdbcSpeedManagementSupportAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Claim claim(UUID tenantId, String scope, String key, String requestHash,
                       UUID targetId, UUID actorId, Instant now) {
        UUID claimId = UUID.nameUUIDFromBytes((tenantId + "|SPEED_RULE|" + scope + "|" + key)
                .getBytes(StandardCharsets.UTF_8));
        String detail = "HASH=" + requestHash + ";VERSION=PENDING";
        int inserted = jdbc.update("""
                INSERT INTO tracking_audit_event(
                 id,tenant_id,actor_id,action,target_type,target_id,safe_detail,occurred_at)
                VALUES(?,?,?,'SPEED_RULE_COMMAND_CLAIMED',?,?,?,?) ON CONFLICT(id) DO NOTHING
                """, claimId, tenantId, actorId, "SPEED_RULE_" + scope, targetId, detail,
                Timestamp.from(now));
        if (inserted == 1) return new Claim(claimId, targetId, requestHash, null, true);
        return jdbc.query("""
                SELECT target_id,safe_detail FROM tracking_audit_event
                WHERE id=? AND tenant_id=? AND action='SPEED_RULE_COMMAND_CLAIMED'
                """, row -> {
                    if (!row.next()) throw conflict();
                    String stored = row.getString("safe_detail");
                    String hash = stored.substring(5, stored.indexOf(';'));
                    String version = stored.substring(stored.indexOf("VERSION=") + 8);
                    return new Claim(claimId, row.getObject("target_id", UUID.class), hash,
                            "PENDING".equals(version) ? null : Long.valueOf(version), false);
                }, claimId, tenantId);
    }

    @Override
    public void complete(UUID claimId, long resultVersion) {
        if (jdbc.update("""
                UPDATE tracking_audit_event
                SET safe_detail=regexp_replace(safe_detail,'VERSION=[^;]+','VERSION=' || ?)
                WHERE id=? AND action='SPEED_RULE_COMMAND_CLAIMED'
                """, Long.toString(resultVersion), claimId) != 1) {
            throw new IllegalStateException("Speed-rule command claim is unavailable");
        }
    }

    @Override
    public void audit(UUID tenantId, UUID actorId, String action, UUID ruleId,
                      String safeDetail, Instant now) {
        jdbc.update("""
                INSERT INTO tracking_audit_event(
                 id,tenant_id,actor_id,action,target_type,target_id,safe_detail,occurred_at)
                VALUES(?,?,?,?,'SPEED_RULE',?,?,?)
                """, UUID.randomUUID(), tenantId, actorId, action, ruleId, safeDetail,
                Timestamp.from(now));
    }

    private static ConflictException conflict() {
        return new ConflictException("IDEMPOTENCY_KEY_CONFLICT",
                "Idempotency-Key was already used for another request");
    }
}
