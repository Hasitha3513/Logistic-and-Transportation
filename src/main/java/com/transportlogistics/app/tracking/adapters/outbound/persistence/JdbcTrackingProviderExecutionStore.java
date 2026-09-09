package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.tracking.application.provider.ClaimedProviderConnection;
import com.transportlogistics.app.tracking.application.provider.DeviceProviderBindingLifecycle;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionId;
import com.transportlogistics.app.tracking.application.provider.ProviderSafeConfiguration;
import com.transportlogistics.app.tracking.application.provider.ProviderType;
import com.transportlogistics.app.tracking.application.provider.TrackingDeviceProviderBinding;
import com.transportlogistics.app.tracking.ports.outbound.TrackingProviderExecutionStore;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
class JdbcTrackingProviderExecutionStore implements TrackingProviderExecutionStore {
    private static final int MAX_CONNECTION_CLAIMS = 100;
    private static final int MAX_BINDING_PAGE = 500;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final ObjectMapper json;

    JdbcTrackingProviderExecutionStore(
            JdbcTemplate jdbc, TransactionTemplate transactions, ObjectMapper json) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.json = json;
    }

    @Override
    public List<ClaimedProviderConnection> claimDueConnections(
            String leaseOwner, Instant now, Duration leaseDuration, int limit) {
        requireOwner(leaseOwner);
        if (limit < 1 || limit > MAX_CONNECTION_CLAIMS) {
            throw new IllegalArgumentException("Connection claim limit must be 1..100");
        }
        if (leaseDuration.isNegative() || leaseDuration.isZero()
                || leaseDuration.compareTo(Duration.ofMinutes(10)) > 0) {
            throw new IllegalArgumentException("Lease duration must be positive and at most 10 minutes");
        }
        return transactions.execute(status -> List.copyOf(jdbc.query("""
                WITH due AS (
                 SELECT id FROM tracking_provider_binding
                 WHERE lifecycle='ACTIVE'
                   AND (next_poll_at IS NULL OR next_poll_at<=?)
                   AND (lease_until IS NULL OR lease_until<=?)
                 ORDER BY next_poll_at NULLS FIRST,id
                 FOR UPDATE SKIP LOCKED
                 LIMIT ?
                )
                UPDATE tracking_provider_binding provider
                SET lease_owner=?,lease_until=?,updated_at=?,version=version+1
                FROM due WHERE provider.id=due.id
                RETURNING provider.*
                """, this::mapConnection, Timestamp.from(now), Timestamp.from(now), limit,
                leaseOwner, Timestamp.from(now.plus(leaseDuration)), Timestamp.from(now))));
    }

    @Override
    public boolean renewLease(
            ProviderConnectionId connectionId,
            String leaseOwner,
            Instant now,
            Duration leaseDuration) {
        requireOwner(leaseOwner);
        return jdbc.update("""
                UPDATE tracking_provider_binding SET lease_until=?,updated_at=?,version=version+1
                WHERE id=? AND lifecycle='ACTIVE' AND lease_owner=? AND lease_until>?
                """, Timestamp.from(now.plus(leaseDuration)), Timestamp.from(now),
                connectionId.value(), leaseOwner, Timestamp.from(now)) == 1;
    }

    @Override
    public boolean hasActiveLease(
            ProviderConnectionId connectionId, String leaseOwner, Instant now) {
        requireOwner(leaseOwner);
        Integer count = jdbc.queryForObject("""
                SELECT count(*) FROM tracking_provider_binding
                WHERE id=? AND lifecycle='ACTIVE' AND lease_owner=? AND lease_until>?
                """, Integer.class, connectionId.value(), leaseOwner, Timestamp.from(now));
        return count != null && count == 1;
    }

    @Override
    public boolean releaseSuccess(
            ProviderConnectionId connectionId,
            String leaseOwner,
            Instant now,
            Instant nextPollAt,
            Instant lastProviderMessageAt) {
        requireOwner(leaseOwner);
        return jdbc.update("""
                UPDATE tracking_provider_binding
                SET lease_owner=NULL,lease_until=NULL,next_poll_at=?,last_successful_poll_at=?,
                    last_provider_message_at=COALESCE(?,last_provider_message_at),
                    last_error_category=NULL,updated_at=?,version=version+1
                WHERE id=? AND lease_owner=?
                """, Timestamp.from(nextPollAt), Timestamp.from(now), timestamp(lastProviderMessageAt),
                Timestamp.from(now), connectionId.value(), leaseOwner) == 1;
    }

    @Override
    public boolean releaseFailure(
            ProviderConnectionId connectionId,
            String leaseOwner,
            Instant now,
            Instant retryAt,
            String errorCategory) {
        requireOwner(leaseOwner);
        if (errorCategory == null || !errorCategory.matches("[A-Z][A-Z0-9_]{0,39}")) {
            throw new IllegalArgumentException("Error category is invalid");
        }
        return jdbc.update("""
                UPDATE tracking_provider_binding
                SET lease_owner=NULL,lease_until=NULL,next_poll_at=?,last_error_category=?,
                    updated_at=?,version=version+1
                WHERE id=? AND lease_owner=?
                """, Timestamp.from(retryAt), errorCategory, Timestamp.from(now),
                connectionId.value(), leaseOwner) == 1;
    }

    @Override
    public Optional<ClaimedProviderConnection> reloadActive(
            ProviderConnectionId connectionId, String leaseOwner, Instant now) {
        requireOwner(leaseOwner);
        return jdbc.query("""
                SELECT * FROM tracking_provider_binding
                WHERE id=? AND lifecycle='ACTIVE' AND lease_owner=? AND lease_until>?
                """, this::mapConnection, connectionId.value(), leaseOwner, Timestamp.from(now))
                .stream().findFirst();
    }

    @Override
    public List<TrackingDeviceProviderBinding> findDueActiveBindings(
            UUID tenantId, ProviderConnectionId connectionId, Instant now, int limit) {
        if (limit < 1 || limit > MAX_BINDING_PAGE) {
            throw new IllegalArgumentException("Binding page must be 1..500");
        }
        return List.copyOf(jdbc.query("""
                SELECT binding.* FROM tracking_device_provider_binding binding
                JOIN tracking_device device
                  ON device.tenant_id=binding.tenant_id AND device.id=binding.tracking_device_id
                JOIN tracking_provider_binding provider
                  ON provider.tenant_id=binding.tenant_id AND provider.id=binding.provider_binding_id
                WHERE binding.tenant_id=? AND binding.provider_binding_id=?
                  AND binding.lifecycle='ACTIVE' AND device.lifecycle='ACTIVE'
                  AND provider.lifecycle='ACTIVE'
                  AND (binding.next_poll_at IS NULL OR binding.next_poll_at<=?)
                ORDER BY binding.next_poll_at NULLS FIRST,binding.id
                LIMIT ?
                """, this::mapBinding, tenantId, connectionId.value(), Timestamp.from(now), limit));
    }

    @Override
    public Optional<TrackingDeviceProviderBinding> lockActiveBindingForIngestion(
            UUID tenantId,
            ProviderConnectionId connectionId,
            String externalDeviceReference,
            String leaseOwner,
            Instant now) {
        requireOwner(leaseOwner);
        return jdbc.query("""
                SELECT binding.* FROM tracking_device_provider_binding binding
                JOIN tracking_device device
                  ON device.tenant_id=binding.tenant_id AND device.id=binding.tracking_device_id
                JOIN tracking_provider_binding provider
                  ON provider.tenant_id=binding.tenant_id AND provider.id=binding.provider_binding_id
                WHERE binding.tenant_id=? AND binding.provider_binding_id=?
                  AND binding.external_device_reference=? AND binding.lifecycle='ACTIVE'
                  AND device.lifecycle='ACTIVE' AND provider.lifecycle='ACTIVE'
                  AND provider.lease_owner=? AND provider.lease_until>?
                FOR SHARE OF binding,device,provider
                """, this::mapBinding, tenantId, connectionId.value(), externalDeviceReference,
                leaseOwner, Timestamp.from(now)).stream().findFirst();
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private ClaimedProviderConnection mapConnection(ResultSet row, int rowNumber) throws SQLException {
        String endpoint = row.getString("endpoint_uri");
        return new ClaimedProviderConnection(
                new ProviderConnectionId(uuid(row, "id")), uuid(row, "tenant_id"),
                ProviderType.of(row.getString("provider_type")), row.getString("provider_alias"),
                row.getString("credential_reference"), endpoint == null ? null : URI.create(endpoint),
                configuration(row.getString("safe_configuration")), row.getInt("poll_interval_seconds"),
                row.getInt("page_size"), row.getString("lease_owner"), instant(row, "lease_until"));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private TrackingDeviceProviderBinding mapBinding(ResultSet row, int rowNumber) throws SQLException {
        return new TrackingDeviceProviderBinding(
                uuid(row, "id"), uuid(row, "tenant_id"), uuid(row, "tracking_device_id"),
                new ProviderConnectionId(uuid(row, "provider_binding_id")),
                row.getString("external_device_reference"),
                configuration(row.getString("safe_configuration")),
                DeviceProviderBindingLifecycle.valueOf(row.getString("lifecycle")),
                instant(row, "watermark_source_timestamp"),
                row.getString("watermark_message_identity"), instant(row, "next_poll_at"),
                instant(row, "created_at"), uuid(row, "created_by"), instant(row, "updated_at"),
                uuid(row, "updated_by"), row.getLong("version"));
    }

    private ProviderSafeConfiguration configuration(String value) {
        try {
            return new ProviderSafeConfiguration(
                    json.readValue(value, new TypeReference<Map<String, String>>() { }));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored provider configuration is invalid", exception);
        }
    }

    private static void requireOwner(String leaseOwner) {
        if (leaseOwner == null || leaseOwner.isBlank() || leaseOwner.length() > 120) {
            throw new IllegalArgumentException("Lease owner is invalid");
        }
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
