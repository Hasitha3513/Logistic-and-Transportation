package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.tracking.application.provider.NewTrackingProviderConnection;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionId;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionLifecycle;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionTestStatus;
import com.transportlogistics.app.tracking.application.provider.ProviderSafeConfiguration;
import com.transportlogistics.app.tracking.application.provider.ProviderType;
import com.transportlogistics.app.tracking.application.provider.TrackingProviderConnection;
import com.transportlogistics.app.tracking.application.provider.TrackingProviderConnectionMutation;
import com.transportlogistics.app.tracking.ports.outbound.TrackingProviderConnectionStore;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
class JdbcTrackingProviderConnectionStore implements TrackingProviderConnectionStore {
    private static final int MAX_SAFE_CONFIGURATION_BYTES = 8_192;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    JdbcTrackingProviderConnectionStore(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public TrackingProviderConnection create(NewTrackingProviderConnection connection) {
        UUID id = UUID.randomUUID();
        String configuration = serialize(connection.safeConfiguration());
        try {
            jdbc.update("""
                    INSERT INTO tracking_provider_binding(
                     id,tenant_id,provider_key_id,provider_alias,credential_reference,lifecycle,
                     created_at,created_by,updated_at,updated_by,version,provider_type,display_name,
                     endpoint_uri,safe_configuration,poll_interval_seconds,page_size,test_status)
                    VALUES(?,?,?,?,?,?,?,?,?,?,0,?,?,?,?::jsonb,?,?,'NOT_TESTED')
                    """, id, connection.tenantId(), connection.providerKeyId(),
                    connection.providerAlias(), connection.credentialReference(),
                    connection.lifecycle().name(), Timestamp.from(connection.now()), connection.actorId(),
                    Timestamp.from(connection.now()), connection.actorId(), connection.providerType().value(),
                    connection.displayName(), uri(connection.endpoint()), configuration,
                    connection.pollIntervalSeconds(), connection.pageSize());
        } catch (DataIntegrityViolationException exception) {
            throw conflict(exception);
        }
        return find(connection.tenantId(), id).orElseThrow();
    }

    @Override
    public Optional<TrackingProviderConnection> find(UUID tenantId, UUID connectionId) {
        List<TrackingProviderConnection> rows = jdbc.query(
                "SELECT * FROM tracking_provider_binding WHERE tenant_id=? AND id=?",
                this::map, tenantId, connectionId);
        return rows.stream().findFirst();
    }

    @Override
    public List<TrackingProviderConnection> list(UUID tenantId) {
        return List.copyOf(jdbc.query("""
                SELECT * FROM tracking_provider_binding
                WHERE tenant_id=? ORDER BY display_name,id
                """, this::map, tenantId));
    }

    @Override
    public TrackingProviderConnection update(
            UUID tenantId,
            UUID connectionId,
            long expectedVersion,
            TrackingProviderConnectionMutation mutation,
            UUID actorId,
            Instant now) {
        TrackingProviderConnection current = find(tenantId, connectionId)
                .orElseThrow(JdbcTrackingProviderConnectionStore::stale);
        if (!current.lifecycle().canTransitionTo(mutation.lifecycle())) {
            throw new BusinessRuleException(
                    "TRACKING_PROVIDER_CONNECTION_INVALID", "Provider lifecycle transition is invalid");
        }
        int changed;
        try {
            changed = jdbc.update("""
                UPDATE tracking_provider_binding SET credential_reference=?,provider_type=?,display_name=?,
                 endpoint_uri=?,safe_configuration=?::jsonb,poll_interval_seconds=?,page_size=?,
                 lifecycle=?,test_status=?,last_tested_at=?,last_successful_poll_at=?,
                 last_provider_message_at=?,last_error_category=?,next_poll_at=?,lease_owner=?,lease_until=?,
                 updated_at=?,updated_by=?,version=version+1
                WHERE tenant_id=? AND id=? AND version=?
                """, mutation.credentialReference(), mutation.providerType().value(),
                mutation.displayName(), uri(mutation.endpoint()), serialize(mutation.safeConfiguration()),
                mutation.pollIntervalSeconds(), mutation.pageSize(), mutation.lifecycle().name(),
                mutation.testStatus().name(), timestamp(mutation.lastTestedAt()),
                timestamp(mutation.lastSuccessfulPollAt()), timestamp(mutation.lastProviderMessageAt()),
                mutation.lastErrorCategory(), timestamp(mutation.nextPollAt()), mutation.leaseOwner(),
                timestamp(mutation.leaseUntil()), Timestamp.from(now), actorId, tenantId, connectionId,
                expectedVersion);
        } catch (DataIntegrityViolationException exception) {
            throw conflict(exception);
        }
        if (changed != 1) {
            throw stale();
        }
        return find(tenantId, connectionId).orElseThrow();
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private TrackingProviderConnection map(ResultSet row, int rowNumber) throws SQLException {
        String endpoint = row.getString("endpoint_uri");
        return new TrackingProviderConnection(
                new ProviderConnectionId(uuid(row, "id")), uuid(row, "tenant_id"),
                row.getString("provider_key_id"), row.getString("provider_alias"),
                row.getString("credential_reference"), ProviderType.of(row.getString("provider_type")),
                row.getString("display_name"), endpoint == null ? null : URI.create(endpoint),
                deserialize(row.getString("safe_configuration")),
                row.getInt("poll_interval_seconds"), row.getInt("page_size"),
                ProviderConnectionLifecycle.valueOf(row.getString("lifecycle")),
                ProviderConnectionTestStatus.valueOf(row.getString("test_status")),
                instant(row, "last_tested_at"), instant(row, "last_successful_poll_at"),
                instant(row, "last_provider_message_at"), row.getString("last_error_category"),
                instant(row, "next_poll_at"), row.getString("lease_owner"),
                instant(row, "lease_until"), instant(row, "created_at"), uuid(row, "created_by"),
                instant(row, "updated_at"), uuid(row, "updated_by"), row.getLong("version"));
    }

    private String serialize(ProviderSafeConfiguration configuration) {
        try {
            String value = json.writeValueAsString(configuration.values());
            if (value.getBytes(StandardCharsets.UTF_8).length > MAX_SAFE_CONFIGURATION_BYTES) {
                throw new IllegalArgumentException("Safe configuration exceeds 8192 encoded bytes");
            }
            return value;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Safe configuration is not serializable", exception);
        }
    }

    private ProviderSafeConfiguration deserialize(String value) {
        try {
            return new ProviderSafeConfiguration(
                    json.readValue(value, new TypeReference<Map<String, String>>() { }));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored provider configuration is invalid", exception);
        }
    }

    private static BusinessRuleException stale() {
        return new BusinessRuleException(
                "TRACKING_STALE_VERSION", "Provider connection is missing or stale");
    }

    private static ConflictException conflict(DataIntegrityViolationException cause) {
        return new ConflictException(
                "TRACKING_PROVIDER_CONNECTION_CONFLICT",
                "Provider connection conflicts with an existing resource", cause);
    }

    private static String uri(URI value) {
        return value == null ? null : value.toASCIIString();
    }

    private static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static UUID uuid(ResultSet row, String column) throws SQLException {
        return UUID.fromString(row.getString(column));
    }

    private static Instant instant(ResultSet row, String column) throws SQLException {
        Timestamp value = row.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
