package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayError;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayException;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.Attribution;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.AttributionStatus;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.BoundaryObservation;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.BoundaryReason;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.Coordinate;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.Coverage;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.CursorBinding;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.CursorPosition;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.CursorState;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.DataGap;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.JourneyPoint;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.Ordering;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.QualityFlag;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ReplayPage;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ReplayBoundaryEvidence;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ReplayQuery;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.TimeRange;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.Trust;
import com.transportlogistics.app.tracking.ports.outbound.JourneyReplayCursorPort;
import com.transportlogistics.app.tracking.ports.outbound.JourneyReplayHistoryPort;
import com.transportlogistics.app.tracking.domain.journeyreplay.StopAnalysisPolicy;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@ConditionalOnProperty(name = "app.tracking.hybrid-storage.enabled", havingValue = "true")
final class JdbcJourneyReplayHistoryAdapter implements JourneyReplayHistoryPort {
    private static final Duration RETENTION = Duration.ofDays(180);
    private static final Duration CURSOR_LIFETIME = Duration.ofMinutes(15);
    private final JdbcTemplate jdbc;
    private final TransactionTemplate readOnly;
    private final JourneyReplayCursorPort cursors;
    private final Clock clock;

    JdbcJourneyReplayHistoryAdapter(JdbcTemplate jdbc, PlatformTransactionManager manager,
            JourneyReplayCursorPort cursors, Clock clock) {
        this.jdbc = jdbc;
        this.cursors = cursors;
        this.clock = clock;
        this.readOnly = new TransactionTemplate(manager);
        this.readOnly.setReadOnly(true);
        this.readOnly.setTimeout(5);
    }

    @Override
    public ReplayPage query(ReplayQuery query, UUID resolvedVehicleId, CursorState cursor) {
        ReplayPage page = readOnly.execute(status -> {
            jdbc.execute("SET LOCAL statement_timeout = '5s'");
            return queryInTransaction(query, resolvedVehicleId, cursor);
        });
        if (page == null) {
            throw new JourneyReplayException(JourneyReplayError.REQUIRED_CAPABILITY_UNAVAILABLE);
        }
        return page;
    }

    private ReplayPage queryInTransaction(
            ReplayQuery query, UUID resolvedVehicleId, CursorState cursor) {
        Instant snapshot = cursor == null ? clock.instant() : cursor.binding().snapshotRecordedAt();
        Instant retentionBoundary = snapshot.minus(RETENTION);
        TimeRange requested = query.effectiveRange();
        Instant availableFrom = requested.from().isBefore(retentionBoundary)
                ? retentionBoundary : requested.from();
        CursorPosition position = cursor == null ? null : cursor.position();
        List<JourneyPoint> rows = jdbc.query("""
                SELECT id,vehicle_id,source_timestamp,received_at,latitude,longitude,
                  horizontal_accuracy_meters,speed_kph,trust,quality,ordering_classification
                FROM tracking_position_history
                WHERE tenant_id=? AND vehicle_id=?
                  AND source_timestamp>=? AND source_timestamp<? AND received_at<=?
                  AND (?::timestamptz IS NULL OR source_timestamp>? OR (source_timestamp=? AND id>?))
                ORDER BY source_timestamp ASC,id ASC
                LIMIT ?
                """, this::map, query.tenant().tenantId(), resolvedVehicleId,
                Timestamp.from(availableFrom), Timestamp.from(requested.to()), Timestamp.from(snapshot),
                position == null ? null : Timestamp.from(position.sourceTimestamp()),
                position == null ? null : Timestamp.from(position.sourceTimestamp()),
                position == null ? null : Timestamp.from(position.sourceTimestamp()),
                position == null ? null : position.historyId(), query.limit() + 1);
        boolean more = rows.size() > query.limit();
        List<JourneyPoint> items = more ? List.copyOf(rows.subList(0, query.limit())) : List.copyOf(rows);
        items = applyContinuity(query.tenant().tenantId(), resolvedVehicleId, availableFrom,
                snapshot, items);
        String next = more ? nextCursor(query, snapshot, items.getLast()) : null;
        boolean partial = requested.from().isBefore(retentionBoundary);
        Coverage coverage = partial ? Coverage.PARTIAL_RETENTION
                : items.isEmpty() ? Coverage.NO_DATA : Coverage.COMPLETE;
        TimeRange available = partial ? new TimeRange(availableFrom, requested.to())
                : coverage == Coverage.NO_DATA ? null : requested;
        List<DataGap> gaps = partial ? List.of(new DataGap(requested.from(), availableFrom,
                Set.of(QualityFlag.PARTIAL_RETENTION))) : List.of();
        ReplayBoundaryEvidence boundaries = boundaries(query.tenant().tenantId(), resolvedVehicleId,
                requested, availableFrom, snapshot, partial);
        return new ReplayPage(items, next, query.requestedRange(), available, coverage, gaps,
                partial, false, Set.of(), snapshot, boundaries);
    }

    private ReplayBoundaryEvidence boundaries(UUID tenantId, UUID vehicleId, TimeRange requested,
            Instant availableFrom, Instant snapshot, boolean retentionTruncated) {
        BoundaryObservation lower;
        if (retentionTruncated) {
            lower = new BoundaryObservation(BoundaryReason.RETENTION_UNAVAILABLE, null);
        } else {
            JourneyPoint predecessor = jdbc.query("""
                    SELECT id,vehicle_id,source_timestamp,received_at,latitude,longitude,
                      horizontal_accuracy_meters,speed_kph,trust,quality,ordering_classification
                    FROM tracking_position_history
                    WHERE tenant_id=? AND vehicle_id=? AND source_timestamp<? AND received_at<=?
                    ORDER BY source_timestamp DESC,id DESC LIMIT 1
                    """, this::map, tenantId, vehicleId, Timestamp.from(availableFrom),
                    Timestamp.from(snapshot)).stream().findFirst().orElse(null);
            lower = observation(predecessor, BoundaryReason.NO_ADJACENT_EVIDENCE);
        }
        JourneyPoint successor = jdbc.query("""
                SELECT id,vehicle_id,source_timestamp,received_at,latitude,longitude,
                  horizontal_accuracy_meters,speed_kph,trust,quality,ordering_classification
                FROM tracking_position_history
                WHERE tenant_id=? AND vehicle_id=? AND source_timestamp>=? AND received_at<=?
                ORDER BY source_timestamp ASC,id ASC LIMIT 1
                """, this::map, tenantId, vehicleId, Timestamp.from(requested.to()),
                Timestamp.from(snapshot)).stream().findFirst().orElse(null);
        BoundaryObservation upper = observation(successor, BoundaryReason.REQUESTED_RANGE_ENDED);
        return new ReplayBoundaryEvidence(lower, upper);
    }

    private static BoundaryObservation observation(JourneyPoint point, BoundaryReason absent) {
        if (point == null) return new BoundaryObservation(absent, null);
        return new BoundaryObservation(StopAnalysisPolicy.eligible(point)
                ? BoundaryReason.ADJACENT_QUALIFYING : BoundaryReason.ADJACENT_INELIGIBLE, point);
    }

    private String nextCursor(ReplayQuery query, Instant snapshot, JourneyPoint last) {
        return cursors.encode(new CursorState(new CursorBinding(query.tenant().tenantId(),
                query.selector(), query.requestedRange(), snapshot),
                new CursorPosition(last.sourceTimestamp(), last.historyId()),
                snapshot.plus(CURSOR_LIFETIME)));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private JourneyPoint map(ResultSet row, int number) throws SQLException {
        try {
            Trust trust = Trust.valueOf(row.getString("trust"));
            Ordering ordering = Ordering.valueOf(row.getString("ordering_classification"));
            BigDecimal latitude = requiredCoordinate(row, "latitude");
            BigDecimal longitude = requiredCoordinate(row, "longitude");
            BigDecimal accuracy = row.getBigDecimal("horizontal_accuracy_meters");
            EnumSet<QualityFlag> flags = EnumSet.noneOf(QualityFlag.class);
            if (trust != Trust.TRUSTED) flags.add(QualityFlag.UNTRUSTED);
            if (accuracy == null) flags.add(QualityFlag.ACCURACY_UNKNOWN);
            else if (accuracy.compareTo(BigDecimal.valueOf(100)) > 0) flags.add(QualityFlag.ACCURACY_LOW);
            if (Duration.between(instant(row, "source_timestamp"), instant(row, "received_at"))
                    .compareTo(Duration.ofMinutes(2)) > 0) flags.add(QualityFlag.STALE_AT_RECEIPT);
            switch (ordering) {
                case OUT_OF_ORDER -> flags.add(QualityFlag.OUT_OF_ORDER);
                case LATE -> flags.add(QualityFlag.LATE);
                case CLOCK_SKEW, FUTURE -> flags.add(QualityFlag.CLOCK_SKEW);
                default -> { }
            }
            flags.add(QualityFlag.ATTRIBUTION_UNKNOWN);
            return new JourneyPoint(row.getObject("id", UUID.class),
                    row.getObject("vehicle_id", UUID.class), instant(row, "source_timestamp"),
                    instant(row, "received_at"), new Coordinate(latitude, longitude),
                    row.getBigDecimal("speed_kph"), accuracy,
                    trust, row.getString("quality"), ordering,
                    Attribution.unavailable(AttributionStatus.UNATTRIBUTED), flags);
        } catch (IllegalArgumentException exception) {
            throw new JourneyReplayException(JourneyReplayError.REQUIRED_CAPABILITY_UNAVAILABLE);
        }
    }

    private static BigDecimal requiredCoordinate(ResultSet row, String column) throws SQLException {
        BigDecimal value = row.getBigDecimal(column);
        if (value == null) {
            throw new JourneyReplayException(JourneyReplayError.REQUIRED_CAPABILITY_UNAVAILABLE);
        }
        return value;
    }

    private static Instant instant(ResultSet row, String column) throws SQLException {
        Timestamp value = row.getTimestamp(column);
        if (value == null) throw new JourneyReplayException(JourneyReplayError.REQUIRED_CAPABILITY_UNAVAILABLE);
        return value.toInstant();
    }

    private List<JourneyPoint> applyContinuity(UUID tenantId, UUID vehicleId, Instant availableFrom,
            Instant snapshot, List<JourneyPoint> items) {
        if (items.isEmpty()) return items;
        JourneyPoint first = items.getFirst();
        JourneyPoint previous = jdbc.query("""
                SELECT id,vehicle_id,source_timestamp,received_at,latitude,longitude,
                  horizontal_accuracy_meters,speed_kph,trust,quality,ordering_classification
                FROM tracking_position_history
                WHERE tenant_id=? AND vehicle_id=? AND source_timestamp>=? AND received_at<=?
                  AND (source_timestamp<? OR (source_timestamp=? AND id<?))
                ORDER BY source_timestamp DESC,id DESC LIMIT 1
                """, this::map, tenantId, vehicleId, Timestamp.from(availableFrom), Timestamp.from(snapshot),
                Timestamp.from(first.sourceTimestamp()), Timestamp.from(first.sourceTimestamp()),
                first.historyId()).stream().findFirst().orElse(null);
        List<JourneyPoint> result = new ArrayList<>(items.size());
        for (JourneyPoint current : items) {
            Set<QualityFlag> flags = new HashSet<>(current.qualityFlags());
            if (previous != null) {
                Duration elapsed = Duration.between(previous.sourceTimestamp(), current.sourceTimestamp());
                if (elapsed.compareTo(Duration.ofMinutes(2)) > 0) flags.add(QualityFlag.TIME_GAP);
                if (!elapsed.isZero() && !elapsed.isNegative()
                        && impliedSpeedKph(previous, current, elapsed) > 200d) {
                    flags.add(QualityFlag.LARGE_JUMP);
                }
            }
            result.add(new JourneyPoint(current.historyId(), current.vehicleId(),
                    current.sourceTimestamp(), current.receivedAt(), current.coordinate(),
                    current.speedKph(), current.accuracyMeters(), current.trust(), current.quality(),
                    current.ordering(), current.attribution(), flags));
            previous = current;
        }
        return List.copyOf(result);
    }

    private static double impliedSpeedKph(
            JourneyPoint first, JourneyPoint second, Duration elapsed) {
        double firstLat = Math.toRadians(first.coordinate().latitude().doubleValue());
        double secondLat = Math.toRadians(second.coordinate().latitude().doubleValue());
        double lat = secondLat - firstLat;
        double lon = Math.toRadians(second.coordinate().longitude().subtract(
                first.coordinate().longitude()).doubleValue());
        double value = Math.sin(lat / 2) * Math.sin(lat / 2)
                + Math.cos(firstLat) * Math.cos(secondLat) * Math.sin(lon / 2) * Math.sin(lon / 2);
        double meters = 12_742_000d * Math.atan2(Math.sqrt(value), Math.sqrt(1 - value));
        return meters / elapsed.toMillis() * 3_600d;
    }
}
