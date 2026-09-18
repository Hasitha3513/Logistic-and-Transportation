package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import com.transportlogistics.app.shared.domain.DependencyUnavailableException;
import com.transportlogistics.app.tracking.application.telemetry.HistoricalTelemetry;
import com.transportlogistics.app.tracking.application.telemetry.CanonicalTelemetryEvent;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV2;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV3;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import com.transportlogistics.app.tracking.domain.TrackingModels.Ordering;
import com.transportlogistics.app.tracking.domain.TrackingModels.Trust;
import com.transportlogistics.app.tracking.ports.outbound.HistoricalTelemetryStorePort;
import com.transportlogistics.app.tracking.ports.outbound.HistoricalTelemetryLookupPort;
import com.transportlogistics.app.tracking.ports.outbound.TelemetryEvaluationDispatchPort;
import com.transportlogistics.app.tracking.ports.outbound.TelemetryCapabilityLookupPort;
import com.transportlogistics.app.tracking.application.provider.TelemetryCapabilityState;
import com.transportlogistics.app.tracking.application.provider.TelemetrySignalCapability;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@ConditionalOnProperty(name = "app.tracking.hybrid-storage.enabled", havingValue = "true")
public final class JdbcHistoricalTelemetryStore implements HistoricalTelemetryStorePort, HistoricalTelemetryLookupPort {
    private static final String RETENTION_POLICY = "TIMESCALE_RAW_180_DAYS";
    private static final String RETENTION_VERSION = "V87";
    private static final Duration RETENTION = Duration.ofDays(180);
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final TelemetryEvaluationDispatchPort dispatch;
    private final TelemetryCapabilityLookupPort capabilities;

    public JdbcHistoricalTelemetryStore(JdbcTemplate jdbc, TransactionTemplate transactions) {
        this(jdbc, transactions, new NoDispatch(), new NoCapabilities());
    }

    public JdbcHistoricalTelemetryStore(JdbcTemplate jdbc, TransactionTemplate transactions,
            TelemetryEvaluationDispatchPort dispatch) {
        this(jdbc, transactions, dispatch, new NoCapabilities());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public JdbcHistoricalTelemetryStore(JdbcTemplate jdbc, TransactionTemplate transactions,
            TelemetryEvaluationDispatchPort dispatch, TelemetryCapabilityLookupPort capabilities) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.dispatch = dispatch;
        this.capabilities = capabilities;
    }

    @Override
    public BatchResult persist(List<? extends CanonicalTelemetryEvent> telemetry) {
        if (telemetry.isEmpty() || telemetry.size() > 500) {
            throw new IllegalArgumentException("Historical telemetry batch size must be 1..500");
        }
        try {
            return transactions.execute(status -> persistAtomically(telemetry));
        } catch (DataAccessException exception) {
            throw new DependencyUnavailableException(
                    "TRACKING_HISTORY_UNAVAILABLE", "Historical telemetry storage is unavailable", exception);
        }
    }

    private BatchResult persistAtomically(List<? extends CanonicalTelemetryEvent> telemetry) {
        int persisted = 0;
        int duplicate = 0;
        int reduced = 0;
        for (var event : telemetry) {
            lock(event.tenantId(), event.vehicleId());
            if (exists(event)) {
                duplicate++;
            } else {
                HistoricalTelemetry candidate = classify(event, latestTrustedSourceTime(
                        event.tenantId(), event.vehicleId()));
                if (reducible(candidate, previous(candidate))) {
                    reduced++;
                } else if (insert(candidate) == 1) {
                    persisted++;
                    dispatch.enqueue(candidate);
                } else {
                    duplicate++;
                    findExact(event.tenantId(), event.recordedAt(), event.eventId())
                            .ifPresent(dispatch::enqueue);
                }
            }
        }
        return new BatchResult(persisted, duplicate, reduced);
    }

    @Override
    public java.util.Optional<HistoricalTelemetry> findExact(
            UUID tenantId, Instant sourceTimestamp, UUID historyId) {
        return jdbc.query("SELECT * FROM tracking_position_history "
                        + "WHERE tenant_id=? AND source_timestamp=? AND id=?",
                this::map, tenantId, Timestamp.from(sourceTimestamp), historyId)
                .stream().findFirst();
    }

    private void lock(UUID tenantId, UUID vehicleId) {
        jdbc.query(
                "SELECT pg_advisory_xact_lock(hashtextextended(? || ':' || ?, 0))",
                row -> { }, tenantId.toString(), vehicleId.toString());
    }

    private boolean exists(CanonicalTelemetryEvent event) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM tracking_position_history "
                        + "WHERE tenant_id=? AND source_timestamp=? AND "
                        + "(id=? OR dedupe_identity=?)",
                Integer.class, event.tenantId(), Timestamp.from(event.recordedAt()),
                event.eventId(), event.dedupeIdentity());
        return count != null && count > 0;
    }

    private HistoricalTelemetry classify(CanonicalTelemetryEvent event, Instant currentTrusted) {
        Duration age = Duration.between(event.recordedAt(), event.receivedAt());
        Ordering ordering = age.compareTo(Duration.ofHours(24)) > 0
                ? Ordering.LATE
                : event.recordedAt().isAfter(event.receivedAt().plusSeconds(120))
                        ? Ordering.FUTURE
                        : event.recordedAt().isAfter(event.receivedAt())
                                ? Ordering.CLOCK_SKEW
                                : currentTrusted != null && event.recordedAt().isBefore(currentTrusted)
                                        ? Ordering.OUT_OF_ORDER : Ordering.IN_ORDER;
        boolean nullIsland = event.latitude().compareTo(BigDecimal.ZERO) == 0
                && event.longitude().compareTo(BigDecimal.ZERO) == 0;
        boolean supportedTamper = tamperState(event) == TrackingTelemetryIngestedV2.TamperState.DETECTED
                && capability(event, TelemetrySignalCapability.TAMPER) == TelemetryCapabilityState.SUPPORTED;
        Trust trust = ordering != Ordering.IN_ORDER || nullIsland || supportedTamper
                ? Trust.UNTRUSTED
                : event.horizontalAccuracyMeters() == null
                        ? Trust.UNKNOWN
                        : event.horizontalAccuracyMeters().compareTo(BigDecimal.valueOf(100)) <= 0
                                ? Trust.TRUSTED : Trust.UNTRUSTED;
        String quality = event.horizontalAccuracyMeters() == null
                ? "ACCURACY_UNKNOWN" : trust == Trust.UNTRUSTED ? "POOR_ACCURACY" : "ACCEPTABLE";
        return new HistoricalTelemetry(event.eventId(), event.eventVersion(), event.tenantId(),
                event.vehicleId(), event.deviceId(), event.providerAlias(), event.providerMessageId(),
                event.dedupeIdentity(), event.latitude(), event.longitude(), event.speedKph(),
                event.headingDegrees(), event.horizontalAccuracyMeters(), event.altitudeMeters(),
                event.engineState(), event.odometerKm(), event.engineHours(), event.recordedAt(),
                event.receivedAt(), trust, quality, ordering,
                tamperState(event), batteryLevel(event), batteryVoltage(event), externalPower(event),
                batteryCharging(event),
                event instanceof TrackingTelemetryIngestedV3 v3 ? v3.ignitionState() : null,
                event instanceof TrackingTelemetryIngestedV3 v3 ? v3.engineRunningState() : null,
                event instanceof TrackingTelemetryIngestedV3 v3 ? v3.engineRunningSource() : null);
    }

    private static TrackingTelemetryIngestedV2.TamperState tamperState(CanonicalTelemetryEvent event) {
        if (event instanceof TrackingTelemetryIngestedV2 v2) {
            return v2.tamperState();
        }
        return event instanceof TrackingTelemetryIngestedV3 v3 ? v3.tamperState() : null;
    }

    private static BigDecimal batteryLevel(CanonicalTelemetryEvent event) {
        if (event instanceof TrackingTelemetryIngestedV2 v2) {
            return v2.batteryLevelPercent();
        }
        return event instanceof TrackingTelemetryIngestedV3 v3 ? v3.batteryLevelPercent() : null;
    }

    private static BigDecimal batteryVoltage(CanonicalTelemetryEvent event) {
        if (event instanceof TrackingTelemetryIngestedV2 v2) {
            return v2.batteryVoltageVolts();
        }
        return event instanceof TrackingTelemetryIngestedV3 v3 ? v3.batteryVoltageVolts() : null;
    }

    private static TrackingTelemetryIngestedV2.ExternalPowerState externalPower(
            CanonicalTelemetryEvent event) {
        if (event instanceof TrackingTelemetryIngestedV2 v2) {
            return v2.externalPowerState();
        }
        return event instanceof TrackingTelemetryIngestedV3 v3 ? v3.externalPowerState() : null;
    }

    private static TrackingTelemetryIngestedV2.BatteryChargingState batteryCharging(
            CanonicalTelemetryEvent event) {
        if (event instanceof TrackingTelemetryIngestedV2 v2) {
            return v2.batteryChargingState();
        }
        return event instanceof TrackingTelemetryIngestedV3 v3 ? v3.batteryChargingState() : null;
    }

    private TelemetryCapabilityState capability(
            CanonicalTelemetryEvent event, TelemetrySignalCapability capability) {
        try {
            return capabilities.resolve(event.tenantId(), event.deviceId(), capability, event.recordedAt());
        } catch (RuntimeException exception) {
            return TelemetryCapabilityState.UNKNOWN;
        }
    }

    private boolean reducible(HistoricalTelemetry candidate, HistoricalTelemetry previous) {
        if (previous == null || candidate.engineState() == null || previous.engineState() == null) {
            return false;
        }
        return candidate.latitude().compareTo(previous.latitude()) == 0
                && candidate.longitude().compareTo(previous.longitude()) == 0
                && candidate.speedKph() != null && candidate.speedKph().signum() == 0
                && candidate.engineState() == previous.engineState()
                && candidate.trust() == previous.trust()
                && candidate.quality().equals(previous.quality())
                && candidate.ordering() == previous.ordering()
                && same(candidate.horizontalAccuracyMeters(), previous.horizontalAccuracyMeters())
                && same(candidate.odometerKm(), previous.odometerKm())
                && same(candidate.engineHours(), previous.engineHours())
                && candidate.ignitionState() == previous.ignitionState()
                && candidate.engineRunningState() == previous.engineRunningState()
                && candidate.engineRunningSource() == previous.engineRunningSource();
    }

    private static boolean same(BigDecimal left, BigDecimal right) {
        return left == null ? right == null : right != null && left.compareTo(right) == 0;
    }

    private Instant latestTrustedSourceTime(UUID tenantId, UUID vehicleId) {
        return jdbc.query(
                "SELECT source_timestamp FROM tracking_position_history "
                        + "WHERE tenant_id=? AND vehicle_id=? AND trust='TRUSTED' "
                        + "ORDER BY source_timestamp DESC,id DESC LIMIT 1",
                (row, number) -> row.getTimestamp(1).toInstant(), tenantId, vehicleId).stream()
                .findFirst().orElse(null);
    }

    private HistoricalTelemetry previous(HistoricalTelemetry candidate) {
        List<HistoricalTelemetry> rows = jdbc.query(
                "SELECT * FROM tracking_position_history WHERE tenant_id=? AND vehicle_id=? "
                        + "AND (source_timestamp<? OR (source_timestamp=? AND id<?)) "
                        + "ORDER BY source_timestamp DESC, id DESC LIMIT 1",
                this::map, candidate.tenantId(), candidate.vehicleId(),
                Timestamp.from(candidate.recordedAt()), Timestamp.from(candidate.recordedAt()),
                candidate.eventId());
        return rows.isEmpty() ? null : rows.get(0);
    }

    private int insert(HistoricalTelemetry fact) {
        return jdbc.update("""
                INSERT INTO tracking_position_history(
                  tenant_id,source_timestamp,id,event_version,device_id,vehicle_id,provider_alias,
                  provider_message_id,dedupe_identity,received_at,latitude,longitude,
                  horizontal_accuracy_meters,speed_kph,heading_degrees,altitude_meters,engine_state,
                  odometer_km,engine_hours,trust,quality,ordering_classification,retention_policy,
                  retention_policy_version,retain_until,safe_metadata,tamper_state,
                  battery_level_percent,battery_voltage_volts,external_power_state,
                  battery_charging_state,ignition_state,engine_running_state,engine_running_source)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'{}'::jsonb,?,?,?,?,?,?,?,?)
                ON CONFLICT DO NOTHING
                """, fact.tenantId(), Timestamp.from(fact.recordedAt()), fact.eventId(),
                fact.eventVersion(), fact.deviceId(), fact.vehicleId(), fact.providerAlias(),
                fact.providerMessageId(), fact.dedupeIdentity(), Timestamp.from(fact.receivedAt()),
                fact.latitude(), fact.longitude(), fact.horizontalAccuracyMeters(), fact.speedKph(),
                fact.headingDegrees(), fact.altitudeMeters(),
                fact.engineState() == null ? EngineState.UNKNOWN.name() : fact.engineState().name(),
                fact.odometerKm(), fact.engineHours(), fact.trust().name(), fact.quality(),
                fact.ordering().name(), RETENTION_POLICY, RETENTION_VERSION,
                Timestamp.from(fact.recordedAt().plus(RETENTION)),
                name(fact.tamperState()), fact.batteryLevelPercent(), fact.batteryVoltageVolts(),
                name(fact.externalPowerState()), name(fact.batteryChargingState()),
                name(fact.ignitionState()), name(fact.engineRunningState()),
                name(fact.engineRunningSource()));
    }

    @Override
    public List<HistoricalTelemetry> find(
            UUID tenantId, UUID vehicleId, Instant fromInclusive, Instant toExclusive, int limit) {
        if (tenantId == null || vehicleId == null || fromInclusive == null || toExclusive == null
                || !fromInclusive.isBefore(toExclusive) || limit < 1 || limit > 500) {
            throw new IllegalArgumentException("Historical telemetry query is invalid");
        }
        return List.copyOf(jdbc.query(
                "SELECT * FROM tracking_position_history WHERE tenant_id=? AND vehicle_id=? "
                        + "AND source_timestamp>=? AND source_timestamp<? "
                        + "ORDER BY source_timestamp DESC,id DESC LIMIT ?",
                this::map, tenantId, vehicleId, Timestamp.from(fromInclusive),
                Timestamp.from(toExclusive), limit));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private HistoricalTelemetry map(ResultSet row, int number) throws SQLException {
        return new HistoricalTelemetry(UUID.fromString(row.getString("id")),
                row.getInt("event_version"), UUID.fromString(row.getString("tenant_id")),
                UUID.fromString(row.getString("vehicle_id")), UUID.fromString(row.getString("device_id")),
                row.getString("provider_alias"), row.getString("provider_message_id"),
                row.getString("dedupe_identity"), row.getBigDecimal("latitude"),
                row.getBigDecimal("longitude"), row.getBigDecimal("speed_kph"),
                row.getBigDecimal("heading_degrees"), row.getBigDecimal("horizontal_accuracy_meters"),
                row.getBigDecimal("altitude_meters"), EngineState.valueOf(row.getString("engine_state")),
                row.getBigDecimal("odometer_km"), row.getBigDecimal("engine_hours"),
                row.getTimestamp("source_timestamp").toInstant(), row.getTimestamp("received_at").toInstant(),
                Trust.valueOf(row.getString("trust")), row.getString("quality"),
                Ordering.valueOf(row.getString("ordering_classification")),
                enumValue(TrackingTelemetryIngestedV2.TamperState.class, row.getString("tamper_state")),
                row.getBigDecimal("battery_level_percent"), row.getBigDecimal("battery_voltage_volts"),
                enumValue(TrackingTelemetryIngestedV2.ExternalPowerState.class,
                        row.getString("external_power_state")),
                enumValue(TrackingTelemetryIngestedV2.BatteryChargingState.class,
                        row.getString("battery_charging_state")),
                enumValue(TrackingTelemetryIngestedV3.IgnitionState.class,
                        row.getString("ignition_state")),
                enumValue(TrackingTelemetryIngestedV3.EngineRunningState.class,
                        row.getString("engine_running_state")),
                enumValue(TrackingTelemetryIngestedV3.EngineRunningSource.class,
                        row.getString("engine_running_source")));
    }

    private static String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value) {
        return value == null ? null : Enum.valueOf(type, value);
    }

    private static final class NoDispatch implements TelemetryEvaluationDispatchPort {
        @Override public void enqueue(HistoricalTelemetry telemetry) { }
        @Override public List<Dispatch> claim(String owner, Instant now, Instant until, int limit) {
            return List.of();
        }
        @Override public List<Dispatch> claimIdle(String owner, Instant now, Instant until, int limit) {
            return List.of();
        }
        @Override public void complete(UUID id, String owner, Instant now) { }
        @Override public void retry(UUID id, String owner, Instant now, Instant next, String code) { }
        @Override public void fail(UUID id, String owner, Instant now, String code) { }
    }

    private static final class NoCapabilities implements TelemetryCapabilityLookupPort {
        @Override
        public TelemetryCapabilityState resolve(UUID tenantId, UUID deviceId,
                TelemetrySignalCapability capability, Instant sourceTimestamp) {
            return TelemetryCapabilityState.UNKNOWN;
        }
    }
}
