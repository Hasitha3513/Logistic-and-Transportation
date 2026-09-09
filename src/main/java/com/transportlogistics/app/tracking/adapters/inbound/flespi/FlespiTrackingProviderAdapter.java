package com.transportlogistics.app.tracking.adapters.inbound.flespi;

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
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
final class FlespiTrackingProviderAdapter implements TrackingProviderAdapter {
    static final ProviderType TYPE = ProviderType.of("FLESPI");
    static final URI DEFAULT_ENDPOINT = URI.create("https://flespi.io");
    static final int MAXIMUM_PAGE_SIZE = 500;
    static final int MAXIMUM_RESPONSE_BYTES = 1_048_576;
    static final Duration MAXIMUM_OVERLAP = Duration.ofMinutes(5);
    private static final String OVERLAP_SECONDS = "overlapSeconds";
    private static final ProviderCapabilities CAPABILITIES = ProviderCapabilities.of(
            ProviderCapability.POLLING,
            ProviderCapability.SOURCE_TIMESTAMP,
            ProviderCapability.ACCURACY,
            ProviderCapability.SPEED,
            ProviderCapability.HEADING,
            ProviderCapability.HISTORY);

    private final FlespiProviderClient client;
    private final FlespiMessageMapper mapper;
    private final FlespiAdapterState state;
    private final MeterRegistry meters;

    FlespiTrackingProviderAdapter(
            FlespiProviderClient client,
            FlespiMessageMapper mapper,
            FlespiAdapterState state,
            MeterRegistry meters) {
        this.client = client;
        this.mapper = mapper;
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
            return invalid("provider_type", "Provider type must be FLESPI");
        }
        URI endpoint = endpoint(configuration.endpoint());
        if (!validEndpoint(endpoint)) {
            return invalid("endpoint", "Flespi endpoint must use HTTPS without query or credentials");
        }
        Map<String, String> values = configuration.safeConfiguration().values();
        if (values.keySet().stream().anyMatch(key -> !OVERLAP_SECONDS.equals(key))) {
            return invalid("safe_configuration", "Unsupported Flespi safe configuration key");
        }
        try {
            overlap(values);
        } catch (IllegalArgumentException exception) {
            return invalid("overlap", "Flespi overlapSeconds must be between 0 and 300");
        }
        return ConfigurationValidation.valid();
    }

    @Override
    public ConnectionTestResult testConnection(
            ProviderConnectionExecution connection, char[] secret) {
        if (!validExecution(connection) || secret == null || secret.length == 0) {
            return new ConnectionTestResult(
                    ConnectionTestResult.Status.INVALID_CONFIGURATION, "configuration_invalid");
        }
        try {
            client.testConnection(endpoint(connection.endpoint()), secret, Duration.ofSeconds(10));
            state.successful(connection.connectionId(), Instant.now(), null);
            return ConnectionTestResult.passed();
        } catch (FlespiFailure failure) {
            state.failed(connection.connectionId(), failure.healthCategory());
            return switch (failure.kind()) {
                case AUTHENTICATION -> new ConnectionTestResult(
                        ConnectionTestResult.Status.AUTH_FAILED, failure.safeCode());
                case TRANSIENT -> new ConnectionTestResult(
                        ConnectionTestResult.Status.UNREACHABLE, failure.safeCode());
                case PERMANENT, MAPPING, DOWNSTREAM -> new ConnectionTestResult(
                        ConnectionTestResult.Status.INVALID_CONFIGURATION, failure.safeCode());
            };
        }
    }

    @Override
    public FetchResult fetchPositions(ProviderFetchRequest request, char[] secret) {
        if (!validExecution(request.connection()) || secret == null || secret.length == 0) {
            throw new FlespiFailure(FlespiFailure.Kind.PERMANENT, "configuration_invalid", null);
        }
        Instant startedAt = Instant.now();
        if (!request.deadline().isAfter(startedAt)) {
            throw new FlespiFailure(FlespiFailure.Kind.TRANSIENT, "provider_deadline", null);
        }
        Timer.Sample latency = Timer.start(meters);
        List<NormalizedPositionCandidate> candidates = new ArrayList<>();
        Map<String, ProviderWatermark> watermarks = new LinkedHashMap<>();
        Instant latest = null;
        try {
            for (var device : request.devices()) {
                if (candidates.size() >= request.pageLimit()) {
                    break;
                }
                Duration timeout = Duration.between(Instant.now(), request.deadline());
                if (timeout.isZero() || timeout.isNegative()) {
                    throw new FlespiFailure(FlespiFailure.Kind.TRANSIENT, "provider_deadline", null);
                }
                Instant to = Instant.now();
                Instant from = device.watermark() == null
                        || device.watermark().sourceTimestamp() == null
                        ? to.minus(overlap(request.connection().safeConfiguration().values()))
                        : device.watermark().sourceTimestamp().minus(
                                overlap(request.connection().safeConfiguration().values()));
                int remaining = request.pageLimit() - candidates.size();
                List<com.fasterxml.jackson.databind.JsonNode> messages = client.fetch(
                        endpoint(request.connection().endpoint()), device.externalDeviceReference(),
                        secret, from, to, remaining, request.responseByteLimit(), timeout);
                meters.counter("tracking.provider.adapter.messages", "provider", TYPE.value(),
                        "result", "received").increment(messages.size());
                Instant deviceLatest = null;
                for (var message : messages) {
                    try {
                        NormalizedPositionCandidate candidate = mapper.map(
                                message, device.externalDeviceReference());
                        candidates.add(candidate);
                        deviceLatest = later(deviceLatest, candidate.sourceTimestamp());
                        latest = later(latest, candidate.sourceTimestamp());
                        meters.counter("tracking.provider.adapter.messages", "provider", TYPE.value(),
                                "result", "mapped").increment();
                    } catch (FlespiFailure failure) {
                        meters.counter("tracking.provider.adapter.messages", "provider", TYPE.value(),
                                "result", "rejected").increment();
                    }
                }
                if (deviceLatest != null) {
                    watermarks.put(device.externalDeviceReference(),
                            new ProviderWatermark(deviceLatest, null));
                }
            }
            candidates.sort(Comparator.comparing(NormalizedPositionCandidate::sourceTimestamp));
            state.successful(request.connection().connectionId(), Instant.now(), latest);
            return new FetchResult(List.copyOf(candidates), Map.copyOf(watermarks));
        } catch (FlespiFailure failure) {
            state.failed(request.connection().connectionId(), failure.healthCategory());
            meters.counter("tracking.provider.adapter.errors", "provider", TYPE.value(),
                    "category", failure.safeCode()).increment();
            throw failure;
        } finally {
            latency.stop(meters.timer("tracking.provider.adapter.fetch.latency",
                    "provider", TYPE.value()));
        }
    }

    @Override
    public ProviderHealth health(ProviderConnectionExecution connection) {
        return state.health(connection.connectionId());
    }

    private boolean validExecution(ProviderConnectionExecution connection) {
        return validateConfiguration(new ProviderConnectionConfiguration(
                connection.providerType(), connection.endpoint(), connection.safeConfiguration()))
                .status() == ConfigurationValidation.Status.VALID;
    }

    private static URI endpoint(URI candidate) {
        return candidate == null ? DEFAULT_ENDPOINT : candidate;
    }

    private static boolean validEndpoint(URI value) {
        return value != null && "https".equalsIgnoreCase(value.getScheme())
                && value.getHost() != null && value.getUserInfo() == null
                && value.getRawQuery() == null && value.getFragment() == null;
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

    private static Instant later(Instant left, Instant right) {
        return left == null || right != null && right.isAfter(left) ? right : left;
    }
}
