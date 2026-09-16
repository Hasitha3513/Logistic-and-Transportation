package com.transportlogistics.app.tracking.application;

import com.transportlogistics.app.tracking.application.provider.TelemetryCapabilityState;
import com.transportlogistics.app.tracking.application.provider.TelemetrySignalCapability;
import com.transportlogistics.app.tracking.application.telemetry.CanonicalTelemetryEvent;
import com.transportlogistics.app.tracking.application.telemetry.LiveTelemetryProjection;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV2;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsCoordinate;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsExceptionEpisode;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsExceptionEvidence;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Assessment;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.EvaluationContext;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.ExceptionType;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Observation;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Quality;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Severity;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.SignalState;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityPolicy;
import com.transportlogistics.app.tracking.ports.inbound.GpsReliabilityEvaluationUseCase;
import com.transportlogistics.app.tracking.ports.outbound.GpsExceptionEvidenceRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.GpsExceptionRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.GpsExceptionTransactionPort;
import com.transportlogistics.app.tracking.ports.outbound.GpsExceptionEventPublisherPort;
import com.transportlogistics.app.tracking.ports.outbound.TelemetryCapabilityLookupPort;
import com.transportlogistics.app.tracking.ports.outbound.GpsDeviceFreshnessPort.DeviceFreshness;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Capability-aware, restart-safe GPS reliability and exception evaluation. */
public final class GpsReliabilityEvaluationService implements GpsReliabilityEvaluationUseCase {
    private static final Duration RETENTION = Duration.ofDays(180);
    private static final Set<TelemetrySignalCapability> OPTIONAL_CAPABILITIES = EnumSet.of(
            TelemetrySignalCapability.TAMPER, TelemetrySignalCapability.BATTERY_LEVEL,
            TelemetrySignalCapability.BATTERY_VOLTAGE, TelemetrySignalCapability.EXTERNAL_POWER,
            TelemetrySignalCapability.BATTERY_CHARGING);
    private final TelemetryCapabilityLookupPort capabilities;
    private final GpsExceptionRepositoryPort episodes;
    private final GpsExceptionEvidenceRepositoryPort evidence;
    private final GpsExceptionTransactionPort transactions;
    private final GpsExceptionEventPublisherPort events;

    public GpsReliabilityEvaluationService(TelemetryCapabilityLookupPort capabilities,
            GpsExceptionRepositoryPort episodes, GpsExceptionEvidenceRepositoryPort evidence,
            GpsExceptionTransactionPort transactions, GpsExceptionEventPublisherPort events) {
        this.capabilities = capabilities;
        this.episodes = episodes;
        this.evidence = evidence;
        this.transactions = transactions;
        this.events = events;
    }

    @Override
    public Assessment evaluate(Observation observation, EvaluationContext context) {
        return GpsReliabilityPolicy.assess(observation, context);
    }

    public Assessment evaluateAndRecord(CanonicalTelemetryEvent event,
            Optional<LiveTelemetryProjection> latest, Instant evaluatedAt) {
        Map<TelemetrySignalCapability, TelemetryCapabilityState> capabilityStates = capabilityStates(event);
        Observation observation = observation(event, capabilityStates);
        Observation previous = latest.map(LiveTelemetryProjection::telemetry)
                .map(value -> observation(value, capabilityStates(value))).orElse(null);
        Assessment assessment = evaluate(observation,
                new EvaluationContext(evaluatedAt, evaluatedAt.minus(RETENTION), previous, true, false));
        Map<ExceptionType, Severity> detected = detected(event, previous, assessment, capabilityStates);
        transactions.execute(() -> {
            apply(event, assessment, detected, evaluatedAt);
            return Boolean.TRUE;
        });
        return assessment;
    }

    public void recordSignalLoss(DeviceFreshness candidate, Instant assessedAt) {
        transactions.execute(() -> {
            if (episodes.findActiveForUpdate(candidate.tenantId(), candidate.deviceId(),
                    ExceptionType.SIGNAL_LOSS).isPresent()) {
                return Boolean.FALSE;
            }
            GpsExceptionEpisode episode = GpsExceptionEpisode.open(UUID.randomUUID(),
                    candidate.tenantId(), candidate.deviceId(), candidate.vehicleId(),
                    ExceptionType.SIGNAL_LOSS, Severity.WARNING, candidate.lastReceivedAt());
            episodes.save(episode);
            String identity = offlineEvidenceIdentity(candidate);
            evidence.append(new GpsExceptionEvidence(UUID.randomUUID(), candidate.tenantId(),
                    episode.id(), identity, null, null, assessedAt,
                    com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Trust.UNKNOWN,
                    com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Ordering.IN_ORDER,
                    com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.ReliabilityState.OFFLINE,
                    "SIGNAL_LOSS", GpsExceptionEvidence.Transition.OPENED, assessedAt));
            events.publishOpened(episode);
            return Boolean.TRUE;
        });
    }

    private void apply(CanonicalTelemetryEvent event, Assessment assessment,
            Map<ExceptionType, Severity> detected, Instant assessedAt) {
        Map<ExceptionType, GpsExceptionEpisode> active = new EnumMap<>(ExceptionType.class);
        episodes.findActiveByDeviceForUpdate(event.tenantId(), event.deviceId())
                .forEach(episode -> active.put(episode.type(), episode));
        detected.forEach((type, severity) -> observe(event, assessment, assessedAt,
                type, severity, active.get(type)));
        if (assessment.latestTrustedEligible()) {
            active.forEach((type, episode) -> {
                if (!detected.containsKey(type) && episode.systemRecoverable()
                        && !event.recordedAt().isBefore(episode.lastObservedAt())) {
                    transition(event, assessment, episode.recover(event.recordedAt()),
                            episode, assessedAt);
                }
            });
        }
    }

    private void observe(CanonicalTelemetryEvent event, Assessment assessment, Instant assessedAt,
            ExceptionType type, Severity severity, GpsExceptionEpisode active) {
        if (active != null && event.recordedAt().isBefore(active.lastObservedAt())) {
            return;
        }
        GpsExceptionEpisode next = active == null
                ? GpsExceptionEpisode.open(UUID.randomUUID(), event.tenantId(), event.deviceId(),
                        event.vehicleId(), type, severity, event.recordedAt())
                : active.observe(severity, event.recordedAt());
        transition(event, assessment, next, active, assessedAt);
    }

    private void transition(CanonicalTelemetryEvent event, Assessment assessment,
            GpsExceptionEpisode next, GpsExceptionEpisode previous, Instant assessedAt) {
        String identity = evidenceIdentity(event, next.type(), next.status().name());
        GpsExceptionEvidence.Transition transition = previous == null
                ? GpsExceptionEvidence.Transition.OPENED
                : next.status() == com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.EpisodeStatus.RESOLVED
                        ? GpsExceptionEvidence.Transition.RESOLVED
                        : next.status() == com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.EpisodeStatus.RECOVERING
                                ? GpsExceptionEvidence.Transition.RECOVERING
                                : GpsExceptionEvidence.Transition.OBSERVED;
        if (previous == null) {
            episodes.save(next);
        }
        boolean appended = evidence.append(new GpsExceptionEvidence(UUID.randomUUID(), event.tenantId(),
                next.id(), identity, event.eventId(), event.recordedAt(), assessedAt,
                assessment.trust(), assessment.ordering(), assessment.state(), qualityCodes(assessment),
                transition, assessedAt));
        if (previous != null && appended) {
            episodes.save(next);
        }
        if (appended && previous == null) {
            events.publishOpened(next);
        } else if (appended && previous.severity() == Severity.WARNING
                && next.severity() == Severity.HIGH) {
            events.publishHigh(next);
        }
    }

    private Map<TelemetrySignalCapability, TelemetryCapabilityState> capabilityStates(
            CanonicalTelemetryEvent event) {
        if (!(event instanceof TrackingTelemetryIngestedV2)) {
            return OPTIONAL_CAPABILITIES.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                    value -> value, value -> TelemetryCapabilityState.UNKNOWN));
        }
        try {
            return capabilities.resolveAll(event.tenantId(), event.deviceId(),
                    OPTIONAL_CAPABILITIES, event.recordedAt());
        } catch (RuntimeException exception) {
            return OPTIONAL_CAPABILITIES.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                    value -> value, value -> TelemetryCapabilityState.UNKNOWN));
        }
    }

    private static Observation observation(CanonicalTelemetryEvent event,
            Map<TelemetrySignalCapability, TelemetryCapabilityState> capabilityStates) {
        TrackingTelemetryIngestedV2 v2 = event instanceof TrackingTelemetryIngestedV2 value ? value : null;
        SignalState tamper = SignalState.UNKNOWN;
        if (v2 != null && supported(capabilityStates, TelemetrySignalCapability.TAMPER)) {
            tamper = v2.tamperState() == TrackingTelemetryIngestedV2.TamperState.DETECTED
                    ? SignalState.ACTIVE
                    : v2.tamperState() == TrackingTelemetryIngestedV2.TamperState.CLEAR
                            ? SignalState.CLEAR : SignalState.UNKNOWN;
        }
        return new Observation(event.tenantId(), event.deviceId(), event.vehicleId(), event.eventId(),
                new GpsCoordinate(event.latitude(), event.longitude()),
                event.horizontalAccuracyMeters() == null ? null
                        : event.horizontalAccuracyMeters().doubleValue(),
                event.recordedAt(), event.receivedAt(), tamper,
                v2 != null && supported(capabilityStates, TelemetrySignalCapability.BATTERY_LEVEL)
                        ? v2.batteryLevelPercent() : null);
    }

    private static Map<ExceptionType, Severity> detected(CanonicalTelemetryEvent event,
            Observation previous, Assessment assessment,
            Map<TelemetrySignalCapability, TelemetryCapabilityState> capabilityStates) {
        Map<ExceptionType, Severity> result = new EnumMap<>(ExceptionType.class);
        if (assessment.qualities().contains(Quality.CLOCK_SKEW)
                || assessment.qualities().contains(Quality.FUTURE)
                || assessment.qualities().contains(Quality.LATE)) {
            result.put(ExceptionType.CLOCK_ANOMALY, Severity.WARNING);
        }
        if (assessment.ordering() != com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Ordering.IN_ORDER) {
            return result;
        }
        if (assessment.qualities().contains(Quality.ACCURACY_UNKNOWN)
                || assessment.qualities().contains(Quality.LOW_ACCURACY)
                || assessment.qualities().contains(Quality.UNUSABLE_ACCURACY)) {
            result.put(ExceptionType.LOW_ACCURACY, Severity.WARNING);
        }
        if (assessment.qualities().contains(Quality.IMPOSSIBLE_MOVEMENT)) {
            result.put(ExceptionType.IMPOSSIBLE_MOVEMENT, Severity.HIGH);
        }
        if (assessment.qualities().contains(Quality.TAMPER_ACTIVE)
                && supported(capabilityStates, TelemetrySignalCapability.TAMPER)) {
            result.put(ExceptionType.DEVICE_TAMPER, Severity.HIGH);
        }
        if (assessment.qualities().contains(Quality.BATTERY_CRITICAL)) {
            result.put(ExceptionType.BATTERY_LOW, Severity.HIGH);
        } else if (assessment.qualities().contains(Quality.BATTERY_LOW)) {
            result.put(ExceptionType.BATTERY_LOW, Severity.WARNING);
        }
        if (event instanceof TrackingTelemetryIngestedV2 current && previous != null
                && supported(capabilityStates, TelemetrySignalCapability.BATTERY_LEVEL)
                && GpsReliabilityPolicy.isRapidBatteryDrain(previous.batteryPercentage(),
                        previous.sourceTimestamp(), current.batteryLevelPercent(), event.recordedAt())) {
            result.put(ExceptionType.BATTERY_RAPID_DRAIN, Severity.WARNING);
        }
        return result;
    }

    private static boolean supported(Map<TelemetrySignalCapability, TelemetryCapabilityState> states,
            TelemetrySignalCapability capability) {
        return states.getOrDefault(capability, TelemetryCapabilityState.UNKNOWN)
                == TelemetryCapabilityState.SUPPORTED;
    }

    private static String qualityCodes(Assessment assessment) {
        return assessment.qualities().stream().map(Enum::name).sorted()
                .collect(java.util.stream.Collectors.joining(","));
    }

    private static String evidenceIdentity(CanonicalTelemetryEvent event,
            ExceptionType type, String transition) {
        String value = event.tenantId() + "|" + event.dedupeIdentity() + "|" + type + "|" + transition;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String offlineEvidenceIdentity(DeviceFreshness candidate) {
        String value = candidate.tenantId() + "|" + candidate.deviceId() + "|SIGNAL_LOSS|"
                + candidate.lastReceivedAt();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
