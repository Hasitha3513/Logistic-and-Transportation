package com.transportlogistics.app.tracking.adapters.inbound.provider;

import com.transportlogistics.app.tracking.application.provider.NormalizedPositionCandidate;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionId;
import com.transportlogistics.app.tracking.application.provider.ProviderIngestionOutcome;
import com.transportlogistics.app.tracking.application.provider.ProviderIngestionOutcome.Result;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV2;
import com.transportlogistics.app.tracking.ports.inbound.TrackingProviderIngestionPort;
import com.transportlogistics.app.tracking.ports.outbound.TelemetryStreamPublisherPort;
import com.transportlogistics.app.tracking.ports.outbound.TelemetryStreamPublisherPort.PublicationRequest;
import com.transportlogistics.app.tracking.ports.outbound.TrackingProviderExecutionStore;
import com.transportlogistics.app.tracking.ports.outbound.TrackingStore;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
final class JdbcTrackingProviderIngestionAdapter implements TrackingProviderIngestionPort {
    private static final int MAX_CANDIDATES = 500;
    private final TrackingProviderExecutionStore executions;
    private final TrackingStore tracking;
    private final TelemetryStreamPublisherPort stream;
    private final Duration acknowledgementTimeout;

    JdbcTrackingProviderIngestionAdapter(
            TrackingProviderExecutionStore executions,
            TrackingStore tracking,
            TelemetryStreamPublisherPort stream,
            @Value("${app.tracking.kafka.acknowledgement-timeout:PT10S}")
            Duration acknowledgementTimeout) {
        this.executions = executions;
        this.tracking = tracking;
        this.stream = stream;
        this.acknowledgementTimeout = acknowledgementTimeout;
    }

    @Override
    public List<ProviderIngestionOutcome> ingest(
            ProviderConnectionId connectionId,
            String leaseOwner,
            List<NormalizedPositionCandidate> candidates,
            Instant receivedAt) {
        if (candidates == null || candidates.isEmpty() || candidates.size() > MAX_CANDIDATES) {
            throw new IllegalArgumentException("Internal provider batch must contain 1..500 candidates");
        }
        var connection = executions.reloadActive(connectionId, leaseOwner, receivedAt);
        if (connection.isEmpty()) {
            return candidates.stream().map(candidate -> rejected(candidate, null, -1)).toList();
        }
        List<PreparedCandidate> prepared = new ArrayList<>(candidates.size());
        for (NormalizedPositionCandidate candidate : candidates) {
            var binding = executions.lockActiveBindingForIngestion(
                    connection.get().tenantId(), connectionId, candidate.externalDeviceReference(),
                    leaseOwner, receivedAt);
            if (binding.isEmpty()) {
                prepared.add(PreparedCandidate.rejected(candidate));
                continue;
            }
            var authority = tracking.ingressDeviceAuthority(
                    connection.get().tenantId(), connectionId.value(),
                    candidate.externalDeviceReference(), candidate.sourceTimestamp());
            if (authority.isEmpty() || !authority.get().deviceId().equals(binding.get().trackingDeviceId())) {
                prepared.add(PreparedCandidate.rejected(candidate));
                continue;
            }
            String identity = identity(connection.get().tenantId(), connection.get().providerAlias(),
                    authority.get().deviceId(), candidate);
            UUID eventId = UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8));
            var event = new TrackingTelemetryIngestedV2(
                    eventId, TrackingTelemetryIngestedV2.TYPE, TrackingTelemetryIngestedV2.VERSION,
                    connection.get().tenantId(), authority.get().vehicleId(), authority.get().deviceId(),
                    connection.get().providerAlias(), candidate.providerMessageId(), identity,
                    candidate.latitude(), candidate.longitude(), candidate.speedKph(),
                    candidate.headingDegrees(), candidate.horizontalAccuracyMeters(),
                    candidate.altitudeMeters(), candidate.engineState(), candidate.odometerKm(),
                    candidate.engineHours(), candidate.sourceTimestamp(), receivedAt,
                    candidate.tamperState(), candidate.batteryLevelPercent(),
                    candidate.batteryVoltageVolts(), candidate.externalPowerState(),
                    candidate.batteryChargingState());
            prepared.add(new PreparedCandidate(candidate, binding.get().id(), binding.get().version(),
                    new PublicationRequest(connection.get().tenantId() + ":" + authority.get().vehicleId(),
                            event, null)));
        }
        List<PublicationRequest> requests = prepared.stream()
                .filter(PreparedCandidate::publishable)
                .map(PreparedCandidate::request)
                .toList();
        if (!requests.isEmpty()) {
            var acknowledgements = stream.publishBatchDurably(requests, acknowledgementTimeout);
            if (acknowledgements.size() != requests.size()) {
                throw new IllegalStateException("Telemetry stream acknowledgement count is invalid");
            }
        }
        return prepared.stream().map(PreparedCandidate::outcome).toList();
    }

    private static ProviderIngestionOutcome rejected(
            NormalizedPositionCandidate candidate, UUID bindingId, long bindingVersion) {
        return new ProviderIngestionOutcome(
                bindingId == null ? new UUID(0, 0) : bindingId,
                bindingVersion,
                candidate.sourceTimestamp(),
                candidate.providerMessageId(),
                Result.REJECTED);
    }

    private static String identity(
            UUID tenantId, String providerAlias, UUID deviceId,
            NormalizedPositionCandidate candidate) {
        String providerIdentity = candidate.providerMessageId() != null
                ? candidate.providerMessageId()
                : deviceId + "|" + candidate.sourceTimestamp() + "|"
                        + candidate.latitude().toPlainString() + "|"
                        + candidate.longitude().toPlainString() + "|" + candidate.providerSequence();
        return sha256(tenantId + "|" + providerAlias + "|" + providerIdentity);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private record PreparedCandidate(
            NormalizedPositionCandidate candidate,
            UUID bindingId,
            long bindingVersion,
            PublicationRequest request) {

        private static PreparedCandidate rejected(NormalizedPositionCandidate candidate) {
            return new PreparedCandidate(candidate, null, -1, null);
        }

        private boolean publishable() {
            return request != null;
        }

        private ProviderIngestionOutcome outcome() {
            return publishable()
                    ? new ProviderIngestionOutcome(bindingId, bindingVersion,
                            candidate.sourceTimestamp(), candidate.providerMessageId(), Result.ACCEPTED)
                    : JdbcTrackingProviderIngestionAdapter.rejected(
                            candidate, bindingId, bindingVersion);
        }
    }
}
