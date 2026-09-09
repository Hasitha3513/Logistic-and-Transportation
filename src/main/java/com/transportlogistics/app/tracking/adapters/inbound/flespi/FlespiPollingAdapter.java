package com.transportlogistics.app.tracking.adapters.inbound.flespi;

import com.transportlogistics.app.integration.IntegrationSecretResolver;
import com.transportlogistics.app.tracking.domain.TrackingModels.ProviderBindingLifecycle;
import com.transportlogistics.app.tracking.ports.outbound.TrackingStore;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
final class FlespiPollingAdapter {
    private static final List<Duration> RETRY_DELAYS = List.of(
            Duration.ofSeconds(5), Duration.ofSeconds(15), Duration.ofSeconds(30), Duration.ofSeconds(60));
    private final FlespiAdapterProperties properties;
    private final TrackingStore store;
    private final IntegrationSecretResolver secrets;
    private final FlespiProviderClient provider;
    private final FlespiMessageMapper mapper;
    private final TrackingIngressBridge ingress;
    private final FlespiAdapterState state;
    private final MeterRegistry meters;
    private final Clock clock;
    private final AtomicBoolean polling = new AtomicBoolean();
    private volatile Instant watermark;
    private volatile Instant nextAttempt = Instant.EPOCH;
    private volatile int retryAttempt;

    FlespiPollingAdapter(FlespiAdapterProperties properties, TrackingStore store,
                         IntegrationSecretResolver secrets, FlespiProviderClient provider,
                         FlespiMessageMapper mapper, TrackingIngressBridge ingress,
                         FlespiAdapterState state, MeterRegistry meters, Clock clock) {
        this.properties = properties;
        this.store = store;
        this.secrets = secrets;
        this.provider = provider;
        this.mapper = mapper;
        this.ingress = ingress;
        this.state = state;
        this.meters = meters;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 5000)
    void scheduledPoll() { poll(false); }

    void pollNow() { poll(true); }

    private void poll(boolean ignoreSchedule) {
        Instant now = clock.instant();
        if (!properties.isEnabled() || !properties.configured()) {
            state.disabled(properties.configured());
            return;
        }
        if ((!ignoreSchedule && now.isBefore(nextAttempt)) || !polling.compareAndSet(false, true)) return;
        Timer.Sample sample = Timer.start(meters);
        char[] secret = null;
        try {
            var binding = store.providerBinding(properties.getProviderKeyId())
                    .filter(value -> value.lifecycle() == ProviderBindingLifecycle.ACTIVE)
                    .filter(value -> value.providerAlias().equalsIgnoreCase(properties.getProviderAlias()))
                    .orElseThrow(() -> new FlespiFailure(
                            FlespiFailure.Kind.AUTHENTICATION, "binding_unavailable", null));
            secret = secrets.resolve(binding.credentialReference()).orElseThrow(() -> new FlespiFailure(
                    FlespiFailure.Kind.AUTHENTICATION, "credential_unavailable", null));

            Instant from = watermark == null ? now.minus(properties.getOverlapWindow()) : watermark;
            var messages = provider.fetch(secret, from, now);
            meters.counter("tracking.flespi.messages", "provider", safeProvider(), "result", "received")
                    .increment(messages.size());
            List<FlespiMessageMapper.MappedPosition> mapped = new ArrayList<>(messages.size());
            for (var message : messages) {
                try {
                    mapped.add(mapper.map(message));
                    meters.counter("tracking.flespi.messages", "provider", safeProvider(), "result", "mapped")
                            .increment();
                } catch (FlespiFailure failure) {
                    meters.counter("tracking.flespi.messages", "provider", safeProvider(), "result", "rejected")
                            .increment();
                    meters.counter("tracking.flespi.mapping.errors", "provider", safeProvider(),
                            "category", failure.safeCode()).increment();
                }
            }
            mapped.sort(java.util.Comparator.comparing(FlespiMessageMapper.MappedPosition::sourceTimestamp));
            if (!mapped.isEmpty()) {
                ingress.ingest(List.copyOf(mapped), secret);
                watermark = mapped.get(mapped.size() - 1).sourceTimestamp();
            }
            retryAttempt = 0;
            nextAttempt = now.plus(properties.getPollInterval());
            Instant lastMessage = mapped.isEmpty() ? state.snapshot().lastProviderMessage()
                    : mapped.get(mapped.size() - 1).sourceTimestamp();
            state.successful(now, lastMessage);
        } catch (FlespiFailure failure) {
            handleFailure(failure, now);
        } finally {
            if (secret != null) Arrays.fill(secret, '\0');
            sample.stop(meters.timer("tracking.flespi.poll.latency", "provider", safeProvider()));
            polling.set(false);
        }
    }

    private void handleFailure(FlespiFailure failure, Instant now) {
        meters.counter("tracking.flespi.provider.errors", "provider", safeProvider(),
                "category", failure.safeCode()).increment();
        if (failure.kind() == FlespiFailure.Kind.TRANSIENT) {
            Duration delay = RETRY_DELAYS.get(Math.min(retryAttempt, RETRY_DELAYS.size() - 1));
            retryAttempt = Math.min(retryAttempt + 1, RETRY_DELAYS.size());
            long jitterMillis = ThreadLocalRandom.current().nextLong(0, 1001);
            nextAttempt = now.plus(delay).plusMillis(jitterMillis);
            meters.counter("tracking.flespi.retries", "provider", safeProvider(), "category", "transient")
                    .increment();
        } else {
            nextAttempt = Instant.MAX;
        }
        FlespiAdapterState.FailureCategory category = switch (failure.kind()) {
            case AUTHENTICATION -> FlespiAdapterState.FailureCategory.AUTHENTICATION;
            case MAPPING, PERMANENT -> FlespiAdapterState.FailureCategory.MAPPING;
            case DOWNSTREAM -> FlespiAdapterState.FailureCategory.DOWNSTREAM;
            case TRANSIENT -> failure.safeCode().startsWith("ingress")
                    ? FlespiAdapterState.FailureCategory.DOWNSTREAM
                    : FlespiAdapterState.FailureCategory.PROVIDER;
        };
        state.failed(category);
    }

    Instant watermark() { return watermark; }
    Instant nextAttempt() { return nextAttempt; }
    int retryAttempt() { return retryAttempt; }

    private String safeProvider() {
        String value = properties.getProviderAlias();
        return value == null || value.isBlank() ? "UNKNOWN" : value.toUpperCase(Locale.ROOT);
    }
}
