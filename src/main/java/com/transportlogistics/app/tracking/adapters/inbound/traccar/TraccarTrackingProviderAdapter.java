package com.transportlogistics.app.tracking.adapters.inbound.traccar;

import com.transportlogistics.app.tracking.application.provider.ConfigurationValidation;
import com.transportlogistics.app.tracking.application.provider.ConfigurationValidation.ValidationIssue;
import com.transportlogistics.app.tracking.application.provider.ConnectionTestResult;
import com.transportlogistics.app.tracking.application.provider.FetchResult;
import com.transportlogistics.app.tracking.application.provider.NormalizedPositionCandidate;
import com.transportlogistics.app.tracking.application.provider.ProviderCapabilities;
import com.transportlogistics.app.tracking.application.provider.ProviderCapability;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionConfiguration;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionExecution;
import com.transportlogistics.app.tracking.application.provider.ProviderFetchRequest;
import com.transportlogistics.app.tracking.application.provider.ProviderHealth;
import com.transportlogistics.app.tracking.application.provider.ProviderType;
import com.transportlogistics.app.tracking.application.provider.ProviderWatermark;
import com.transportlogistics.app.tracking.ports.outbound.TrackingProviderAdapter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
final class TraccarTrackingProviderAdapter implements TrackingProviderAdapter {
    static final ProviderType TYPE = ProviderType.of("TRACCAR");
    static final int MAXIMUM_RESPONSE_BYTES = 1_048_576;
    static final Duration MAXIMUM_OVERLAP = Duration.ofMinutes(5);
    private static final String OVERLAP_SECONDS = "overlapSeconds";
    private static final ProviderCapabilities CAPABILITIES = ProviderCapabilities.of(
            ProviderCapability.POLLING, ProviderCapability.SOURCE_TIMESTAMP,
            ProviderCapability.ACCURACY, ProviderCapability.SPEED, ProviderCapability.HEADING,
            ProviderCapability.IGNITION, ProviderCapability.ODOMETER, ProviderCapability.MESSAGE_ID,
            ProviderCapability.HISTORY);

    private final TraccarProviderClient client;
    private final TraccarMessageMapper mapper;
    private final TraccarEndpointPolicy endpoints;
    private final TraccarAdapterState state;
    private final MeterRegistry meters;

    TraccarTrackingProviderAdapter(
            TraccarProviderClient client, TraccarMessageMapper mapper,
            TraccarEndpointPolicy endpoints, TraccarAdapterState state, MeterRegistry meters) {
        this.client = client;
        this.mapper = mapper;
        this.endpoints = endpoints;
        this.state = state;
        this.meters = meters;
    }

    @Override
    public ProviderType providerType() {
        return TYPE;
    }

    @Override
    public ProviderCapabilities capabilities() {
        return CAPABILITIES;
    }

    @Override
    public ConfigurationValidation validateConfiguration(
            ProviderConnectionConfiguration configuration) {
        if (!TYPE.equals(configuration.providerType())) {
            return invalid("provider_type", "Provider type must be TRACCAR");
        }
        if (!endpoints.structurallyAllowed(configuration.endpoint())) {
            return invalid("endpoint", "Traccar endpoint is not permitted");
        }
        Map<String, String> values = configuration.safeConfiguration().values();
        if (values.keySet().stream().anyMatch(key -> !OVERLAP_SECONDS.equals(key))) {
            return invalid("safe_configuration", "Unsupported Traccar safe configuration key");
        }
        try {
            overlap(values);
        } catch (IllegalArgumentException exception) {
            return invalid("overlap", "Traccar overlapSeconds must be between 0 and 300");
        }
        return ConfigurationValidation.valid();
    }

    @Override
    public ConnectionTestResult testConnection(
            ProviderConnectionExecution connection, char[] secret) {
        if (!valid(connection) || secret == null || secret.length == 0) {
            return new ConnectionTestResult(
                    ConnectionTestResult.Status.INVALID_CONFIGURATION, "configuration_invalid");
        }
        try {
            client.testConnection(connection.endpoint(), secret, Duration.ofSeconds(10));
            state.successful(connection.connectionId(), Instant.now());
            return ConnectionTestResult.passed();
        } catch (TraccarFailure failure) {
            state.failed(connection.connectionId(), failure.healthCategory());
            return switch (failure.kind()) {
                case AUTHENTICATION -> new ConnectionTestResult(
                        ConnectionTestResult.Status.AUTH_FAILED, failure.safeCode());
                case TRANSIENT -> new ConnectionTestResult(
                        ConnectionTestResult.Status.UNREACHABLE, failure.safeCode());
                case PERMANENT, MAPPING -> new ConnectionTestResult(
                        ConnectionTestResult.Status.INVALID_CONFIGURATION, failure.safeCode());
            };
        }
    }

    @Override
    public FetchResult fetchPositions(ProviderFetchRequest request, char[] secret) {
        if (!valid(request.connection()) || secret == null || secret.length == 0) {
            throw new TraccarFailure(
                    TraccarFailure.Kind.PERMANENT, "configuration_invalid", null);
        }
        Timer.Sample latency = Timer.start(meters);
        List<NormalizedPositionCandidate> candidates = new ArrayList<>();
        Map<String, ProviderWatermark> watermarks = new LinkedHashMap<>();
        try {
            for (var device : request.devices()) {
                if (candidates.size() >= request.pageLimit()) {
                    throw new TraccarFailure(
                            TraccarFailure.Kind.PERMANENT, "provider_response_overflow", null);
                }
                Duration timeout = Duration.between(Instant.now(), request.deadline());
                if (timeout.isZero() || timeout.isNegative()) {
                    throw new TraccarFailure(
                            TraccarFailure.Kind.TRANSIENT, "provider_deadline", null);
                }
                Instant to = Instant.now();
                Instant from = device.watermark() == null
                        || device.watermark().sourceTimestamp() == null
                        ? to.minus(overlap(request.connection().safeConfiguration().values()))
                        : device.watermark().sourceTimestamp().minus(
                                overlap(request.connection().safeConfiguration().values()));
                int remaining = request.pageLimit() - candidates.size();
                List<com.fasterxml.jackson.databind.JsonNode> positions = client.fetch(
                        request.connection().endpoint(), device.externalDeviceReference(), secret,
                        from, to, remaining, request.responseByteLimit(), timeout);
                List<NormalizedPositionCandidate> mapped = positions.stream()
                        .map(position -> mapper.map(position, device.externalDeviceReference()))
                        .sorted(Comparator.comparing(NormalizedPositionCandidate::sourceTimestamp)
                                .thenComparingLong(candidate -> candidate.providerSequence() == null
                                        ? Long.MIN_VALUE : candidate.providerSequence()))
                        .toList();
                candidates.addAll(mapped);
                if (!mapped.isEmpty()) {
                    var last = mapped.getLast();
                    watermarks.put(device.externalDeviceReference(),
                            new ProviderWatermark(last.sourceTimestamp(), last.providerMessageId()));
                }
            }
            candidates.sort(Comparator.comparing(NormalizedPositionCandidate::sourceTimestamp)
                    .thenComparingLong(candidate -> candidate.providerSequence() == null
                            ? Long.MIN_VALUE : candidate.providerSequence()));
            state.successful(request.connection().connectionId(), Instant.now());
            return new FetchResult(List.copyOf(candidates), Map.copyOf(watermarks));
        } catch (TraccarFailure failure) {
            state.failed(request.connection().connectionId(), failure.healthCategory());
            meters.counter("tracking.provider.adapter.errors", "provider", TYPE.value(),
                    "category", failure.safeCode()).increment();
            throw failure;
        } finally {
            latency.stop(meters.timer(
                    "tracking.provider.adapter.fetch.latency", "provider", TYPE.value()));
        }
    }

    @Override
    public ProviderHealth health(ProviderConnectionExecution connection) {
        return state.health(connection.connectionId());
    }

    private boolean valid(ProviderConnectionExecution connection) {
        return validateConfiguration(new ProviderConnectionConfiguration(
                connection.providerType(), connection.endpoint(), connection.safeConfiguration()))
                .status() == ConfigurationValidation.Status.VALID;
    }

    private static Duration overlap(Map<String, String> values) {
        String configured = values.get(OVERLAP_SECONDS);
        if (configured == null) {
            return MAXIMUM_OVERLAP;
        }
        int seconds = Integer.parseInt(configured);
        if (seconds < 0 || seconds > MAXIMUM_OVERLAP.toSeconds()) {
            throw new IllegalArgumentException("overlapSeconds is outside bounds");
        }
        return Duration.ofSeconds(seconds);
    }

    private static ConfigurationValidation invalid(String code, String message) {
        return ConfigurationValidation.invalid(new ValidationIssue(code, message));
    }
}
