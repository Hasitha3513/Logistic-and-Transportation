package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.tracking.application.provider.DeviceProviderBindingLifecycle;
import com.transportlogistics.app.tracking.application.provider.NewTrackingDeviceProviderBinding;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionId;
import com.transportlogistics.app.tracking.application.provider.ProviderSafeConfiguration;
import com.transportlogistics.app.tracking.application.provider.TrackingDeviceProviderBinding;
import com.transportlogistics.app.tracking.ports.outbound.TrackingDeviceProviderBindingStore;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
class JdbcTrackingDeviceProviderBindingStore implements TrackingDeviceProviderBindingStore {
    private static final int MAX_SAFE_CONFIGURATION_BYTES = 4_096;
    private static final int MAX_LIST_SIZE = 500;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final ObjectMapper json;

    JdbcTrackingDeviceProviderBindingStore(
            JdbcTemplate jdbc, TransactionTemplate transactions, ObjectMapper json) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.json = json;
    }

    @Override
    public TrackingDeviceProviderBinding create(NewTrackingDeviceProviderBinding binding) {
        return transactions.execute(status -> createInTransaction(binding));
    }

    @Override
    public Optional<TrackingDeviceProviderBinding> find(UUID tenantId, UUID bindingId) {
        return one("""
                SELECT * FROM tracking_device_provider_binding
                WHERE tenant_id=? AND id=?
                """, tenantId, bindingId);
    }

    @Override
    public Optional<TrackingDeviceProviderBinding> findActiveByDevice(
            UUID tenantId, UUID trackingDeviceId) {
        return one("""
                SELECT * FROM tracking_device_provider_binding
                WHERE tenant_id=? AND tracking_device_id=? AND lifecycle='ACTIVE'
                """, tenantId, trackingDeviceId);
    }

    @Override
    public Optional<TrackingDeviceProviderBinding> findByExternalReference(
            UUID tenantId, ProviderConnectionId connectionId, String externalDeviceReference) {
        String reference = TrackingDeviceProviderBinding.required(
                externalDeviceReference, "externalDeviceReference", 160);
        return one("""
                SELECT * FROM tracking_device_provider_binding
                WHERE tenant_id=? AND provider_binding_id=? AND external_device_reference=?
                """, tenantId, connectionId.value(), reference);
    }

    @Override
    public List<TrackingDeviceProviderBinding> listByProviderConnection(
            UUID tenantId, ProviderConnectionId connectionId, int limit) {
        if (limit < 1 || limit > MAX_LIST_SIZE) {
            throw new IllegalArgumentException("limit must be between 1 and 500");
        }
        return List.copyOf(jdbc.query("""
                SELECT * FROM tracking_device_provider_binding
                WHERE tenant_id=? AND provider_binding_id=?
                ORDER BY lifecycle,id LIMIT ?
                """, this::map, tenantId, connectionId.value(), limit));
    }

    @Override
    public TrackingDeviceProviderBinding updateLifecycle(
            UUID tenantId,
            UUID bindingId,
            long expectedVersion,
            DeviceProviderBindingLifecycle lifecycle,
            UUID actorId,
            Instant now) {
        return transactions.execute(status -> {
            TrackingDeviceProviderBinding current = find(tenantId, bindingId).orElseThrow(
                    JdbcTrackingDeviceProviderBindingStore::stale);
            if (!current.lifecycle().canTransitionTo(lifecycle)) {
                throw invalid("Device-provider binding lifecycle transition is invalid");
            }
            if (lifecycle == DeviceProviderBindingLifecycle.ACTIVE) {
                validateActivation(tenantId, current.trackingDeviceId(), current.providerConnectionId());
            }
            updateLifecycleRow(tenantId, bindingId, expectedVersion, lifecycle, actorId, now);
            audit(tenantId, actorId, "DEVICE_PROVIDER_BINDING_" + lifecycle.name(), bindingId,
                    "FROM=" + current.lifecycle().name(), now);
            if (lifecycle == DeviceProviderBindingLifecycle.ACTIVE) {
                updateCompatibilityProjection(current, now);
            }
            return find(tenantId, bindingId).orElseThrow();
        });
    }

    @Override
    public TrackingDeviceProviderBinding rebind(
            NewTrackingDeviceProviderBinding replacement, long expectedCurrentVersion) {
        if (replacement.lifecycle() != DeviceProviderBindingLifecycle.ACTIVE) {
            throw invalid("Replacement binding must be ACTIVE");
        }
        return transactions.execute(status -> {
            lockDevice(replacement.tenantId(), replacement.trackingDeviceId());
            TrackingDeviceProviderBinding current = activeForUpdate(
                    replacement.tenantId(), replacement.trackingDeviceId()).orElseThrow(
                            JdbcTrackingDeviceProviderBindingStore::stale);
            if (current.version() != expectedCurrentVersion) {
                throw stale();
            }
            validateActivation(replacement.tenantId(), replacement.trackingDeviceId(),
                    replacement.providerConnectionId());
            updateLifecycleRow(replacement.tenantId(), current.id(), expectedCurrentVersion,
                    DeviceProviderBindingLifecycle.DISABLED, replacement.actorId(), replacement.now());
            TrackingDeviceProviderBinding created = createInTransaction(replacement);
            audit(replacement.tenantId(), replacement.actorId(), "DEVICE_PROVIDER_REBOUND",
                    replacement.trackingDeviceId(), "RESULT=ACTIVE", replacement.now());
            return created;
        });
    }

    @Override
    public TrackingDeviceProviderBinding updateWatermark(
            UUID tenantId,
            UUID bindingId,
            long expectedVersion,
            Instant sourceTimestamp,
            String messageIdentity,
            Instant nextPollAt,
            UUID actorId,
            Instant now) {
        String identity = TrackingDeviceProviderBinding.optional(messageIdentity, 160);
        int changed = jdbc.update("""
                UPDATE tracking_device_provider_binding
                SET watermark_source_timestamp=?,watermark_message_identity=?,next_poll_at=?,
                    updated_at=?,updated_by=?,version=version+1
                WHERE tenant_id=? AND id=? AND version=?
                """, timestamp(sourceTimestamp), identity, timestamp(nextPollAt), Timestamp.from(now),
                actorId, tenantId, bindingId, expectedVersion);
        changed(changed);
        return find(tenantId, bindingId).orElseThrow();
    }

    @Override
    public TrackingDeviceProviderBinding updateNextPoll(
            UUID tenantId,
            UUID bindingId,
            long expectedVersion,
            Instant nextPollAt,
            UUID actorId,
            Instant now) {
        int changed = jdbc.update("""
                UPDATE tracking_device_provider_binding
                SET next_poll_at=?,updated_at=?,updated_by=?,version=version+1
                WHERE tenant_id=? AND id=? AND version=?
                """, timestamp(nextPollAt), Timestamp.from(now), actorId, tenantId, bindingId,
                expectedVersion);
        changed(changed);
        return find(tenantId, bindingId).orElseThrow();
    }

    private TrackingDeviceProviderBinding createInTransaction(NewTrackingDeviceProviderBinding binding) {
        if (binding.lifecycle() == DeviceProviderBindingLifecycle.ACTIVE) {
            validateActivation(binding.tenantId(), binding.trackingDeviceId(),
                    binding.providerConnectionId());
        } else {
            validateParentsExist(binding.tenantId(), binding.trackingDeviceId(),
                    binding.providerConnectionId());
        }
        UUID id = UUID.randomUUID();
        try {
            jdbc.update("""
                    INSERT INTO tracking_device_provider_binding(
                     id,tenant_id,tracking_device_id,provider_binding_id,external_device_reference,
                     safe_configuration,lifecycle,next_poll_at,created_at,updated_at,created_by,updated_by)
                    VALUES(?,?,?,?,?,?::jsonb,?,?,?,?,?,?)
                    """, id, binding.tenantId(), binding.trackingDeviceId(),
                    binding.providerConnectionId().value(), binding.externalDeviceReference(),
                    serialize(binding.safeConfiguration()), binding.lifecycle().name(),
                    timestamp(binding.nextPollAt()), Timestamp.from(binding.now()),
                    Timestamp.from(binding.now()), binding.actorId(), binding.actorId());
        } catch (DataIntegrityViolationException exception) {
            throw conflict();
        }
        TrackingDeviceProviderBinding created = find(binding.tenantId(), id).orElseThrow();
        audit(binding.tenantId(), binding.actorId(), "DEVICE_PROVIDER_BOUND", id,
                "LIFECYCLE=" + binding.lifecycle().name(), binding.now());
        if (binding.lifecycle() == DeviceProviderBindingLifecycle.ACTIVE) {
            updateCompatibilityProjection(created, binding.now());
        }
        return created;
    }

    private void validateParentsExist(
            UUID tenantId, UUID deviceId, ProviderConnectionId connectionId) {
        deviceLifecycle(tenantId, deviceId);
        provider(tenantId, connectionId);
    }

    private void validateActivation(
            UUID tenantId, UUID deviceId, ProviderConnectionId connectionId) {
        if (!"ACTIVE".equals(deviceLifecycle(tenantId, deviceId))) {
            throw invalid("Tracking device is not active");
        }
        if (!"ACTIVE".equals(provider(tenantId, connectionId).lifecycle())) {
            throw invalid("Provider connection is not active");
        }
    }

    private String deviceLifecycle(UUID tenantId, UUID deviceId) {
        List<String> rows = jdbc.query("""
                SELECT lifecycle FROM tracking_device WHERE tenant_id=? AND id=?
                """, (row, number) -> row.getString(1), tenantId, deviceId);
        if (rows.isEmpty()) {
            throw invalid("Tracking device or Tenant is invalid");
        }
        return rows.get(0);
    }

    private ProviderAuthority provider(UUID tenantId, ProviderConnectionId connectionId) {
        List<ProviderAuthority> rows = jdbc.query("""
                SELECT lifecycle,provider_alias FROM tracking_provider_binding
                WHERE tenant_id=? AND id=?
                """, (row, number) -> new ProviderAuthority(row.getString(1), row.getString(2)),
                tenantId, connectionId.value());
        if (rows.isEmpty()) {
            throw invalid("Provider connection or Tenant is invalid");
        }
        return rows.get(0);
    }

    private void updateCompatibilityProjection(TrackingDeviceProviderBinding binding, Instant now) {
        ProviderAuthority authority = provider(binding.tenantId(), binding.providerConnectionId());
        int changed = jdbc.update("""
                UPDATE tracking_device
                SET provider_alias=?,external_device_reference=?,updated_at=?,version=version+1
                WHERE tenant_id=? AND id=?
                """, authority.alias(), binding.externalDeviceReference(), Timestamp.from(now),
                binding.tenantId(), binding.trackingDeviceId());
        if (changed != 1) {
            throw invalid("Tracking device or Tenant is invalid");
        }
    }

    private void updateLifecycleRow(
            UUID tenantId,
            UUID bindingId,
            long expectedVersion,
            DeviceProviderBindingLifecycle lifecycle,
            UUID actorId,
            Instant now) {
        try {
            int changed = jdbc.update("""
                    UPDATE tracking_device_provider_binding
                    SET lifecycle=?,updated_at=?,updated_by=?,version=version+1
                    WHERE tenant_id=? AND id=? AND version=?
                    """, lifecycle.name(), Timestamp.from(now), actorId, tenantId, bindingId,
                    expectedVersion);
            changed(changed);
        } catch (DataIntegrityViolationException exception) {
            throw conflict();
        }
    }

    private void audit(
            UUID tenantId, UUID actorId, String action, UUID targetId, String detail, Instant now) {
        jdbc.update("""
                INSERT INTO tracking_audit_event(
                 id,tenant_id,actor_id,action,target_type,target_id,safe_detail,occurred_at)
                VALUES(?,?,?,?,?,?,?,?)
                """, UUID.randomUUID(), tenantId, actorId, action, "DEVICE_PROVIDER_BINDING",
                targetId, detail, Timestamp.from(now));
    }

    private void lockDevice(UUID tenantId, UUID deviceId) {
        List<UUID> rows = jdbc.query("""
                SELECT id FROM tracking_device WHERE tenant_id=? AND id=? FOR UPDATE
                """, (row, number) -> UUID.fromString(row.getString(1)), tenantId, deviceId);
        if (rows.isEmpty()) {
            throw invalid("Tracking device or Tenant is invalid");
        }
    }

    private Optional<TrackingDeviceProviderBinding> activeForUpdate(UUID tenantId, UUID deviceId) {
        List<TrackingDeviceProviderBinding> rows = jdbc.query("""
                SELECT * FROM tracking_device_provider_binding
                WHERE tenant_id=? AND tracking_device_id=? AND lifecycle='ACTIVE' FOR UPDATE
                """, this::map, tenantId, deviceId);
        return rows.stream().findFirst();
    }

    private Optional<TrackingDeviceProviderBinding> one(String sql, Object... arguments) {
        return jdbc.query(sql, this::map, arguments).stream().findFirst();
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private TrackingDeviceProviderBinding map(ResultSet row, int rowNumber) throws SQLException {
        return new TrackingDeviceProviderBinding(
                uuid(row, "id"), uuid(row, "tenant_id"), uuid(row, "tracking_device_id"),
                new ProviderConnectionId(uuid(row, "provider_binding_id")),
                row.getString("external_device_reference"),
                deserialize(row.getString("safe_configuration")),
                DeviceProviderBindingLifecycle.valueOf(row.getString("lifecycle")),
                instant(row, "watermark_source_timestamp"),
                row.getString("watermark_message_identity"), instant(row, "next_poll_at"),
                instant(row, "created_at"), uuid(row, "created_by"), instant(row, "updated_at"),
                uuid(row, "updated_by"), row.getLong("version"));
    }

    private String serialize(ProviderSafeConfiguration configuration) {
        try {
            String value = json.writeValueAsString(configuration.values());
            if (value.getBytes(StandardCharsets.UTF_8).length > MAX_SAFE_CONFIGURATION_BYTES) {
                throw new IllegalArgumentException("Safe configuration exceeds 4096 encoded bytes");
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
            throw new IllegalStateException("Stored device configuration is invalid", exception);
        }
    }

    private static void changed(int changed) {
        if (changed != 1) {
            throw stale();
        }
    }

    private static BusinessRuleException stale() {
        return new BusinessRuleException(
                "TRACKING_STALE_VERSION", "Device-provider binding is missing or stale");
    }

    private static BusinessRuleException invalid(String message) {
        return new BusinessRuleException("TRACKING_DEVICE_PROVIDER_INVALID", message);
    }

    private static BusinessRuleException conflict() {
        return new BusinessRuleException(
                "TRACKING_DEVICE_EXTERNAL_ID_CONFLICT", "Device-provider binding conflicts");
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

    private record ProviderAuthority(String lifecycle, String alias) { }
}
