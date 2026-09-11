package com.transportlogistics.app.tracking.adapters.inbound.provider;

import com.transportlogistics.app.integration.IntegrationSecretResolver;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.tracking.adapters.configuration.ProviderCoordinatorSettings;
import com.transportlogistics.app.tracking.application.provider.ClaimedProviderConnection;
import com.transportlogistics.app.tracking.application.provider.ProviderDeviceCursor;
import com.transportlogistics.app.tracking.application.provider.ProviderFetchRequest;
import com.transportlogistics.app.tracking.application.provider.ProviderIngestionOutcome;
import com.transportlogistics.app.tracking.application.provider.ProviderIngestionOutcome.Result;
import com.transportlogistics.app.tracking.application.provider.ProviderType;
import com.transportlogistics.app.tracking.application.provider.ProviderWatermark;
import com.transportlogistics.app.tracking.application.provider.TrackingDeviceProviderBinding;
import com.transportlogistics.app.tracking.application.provider.TrackingProviderAdapterRegistry;
import com.transportlogistics.app.tracking.ports.inbound.TrackingProviderIngestionPort;
import com.transportlogistics.app.tracking.ports.outbound.TrackingDeviceProviderBindingStore;
import com.transportlogistics.app.tracking.ports.outbound.TrackingProviderExecutionStore;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.tracking.provider-coordinator.enabled", havingValue = "true")
public final class ProviderPollingCoordinator {
    private static final String FAILURE_UNSUPPORTED = "PROVIDER_UNSUPPORTED";
    private static final String FAILURE_CREDENTIAL = "CREDENTIAL_UNAVAILABLE";
    private static final String FAILURE_PROVIDER = "PROVIDER_FAILURE";
    private static final String FAILURE_SATURATED = "WORKER_SATURATED";
    private final TrackingProviderExecutionStore executions;
    private final TrackingDeviceProviderBindingStore bindings;
    private final TrackingProviderAdapterRegistry adapters;
    private final TrackingProviderIngestionPort ingestion;
    private final IntegrationSecretResolver secrets;
    private final ProviderCoordinatorSettings settings;
    private final ProviderCoordinatorState state;
    private final MeterRegistry meters;
    private final Clock clock;
    private final String leaseOwner;
    private final ThreadPoolExecutor workers;
    private final Set<UUID> localFlights = ConcurrentHashMap.newKeySet();
    private final Map<ProviderType, Semaphore> providerQuotas = new ConcurrentHashMap<>();
    private final Map<UUID, ProviderType> activeConnections = new ConcurrentHashMap<>();
    private volatile boolean accepting = true;

    public ProviderPollingCoordinator(
            TrackingProviderExecutionStore executions,
            TrackingDeviceProviderBindingStore bindings,
            TrackingProviderAdapterRegistry adapters,
            TrackingProviderIngestionPort ingestion,
            IntegrationSecretResolver secrets,
            ProviderCoordinatorSettings settings,
            ProviderCoordinatorState state,
            MeterRegistry meters,
            Clock clock) {
        this(executions, bindings, adapters, ingestion, secrets, settings, state, meters, clock,
                "tracking-" + UUID.randomUUID());
    }

    ProviderPollingCoordinator(
            TrackingProviderExecutionStore executions,
            TrackingDeviceProviderBindingStore bindings,
            TrackingProviderAdapterRegistry adapters,
            TrackingProviderIngestionPort ingestion,
            IntegrationSecretResolver secrets,
            ProviderCoordinatorSettings settings,
            ProviderCoordinatorState state,
            MeterRegistry meters,
            Clock clock,
            String leaseOwner) {
        this.executions = executions;
        this.bindings = bindings;
        this.adapters = adapters;
        this.ingestion = ingestion;
        this.secrets = secrets;
        this.settings = settings;
        this.state = state;
        this.meters = meters;
        this.clock = clock;
        this.leaseOwner = leaseOwner;
        BlockingQueue<Runnable> queue = settings.queueCapacity() == 0
                ? new SynchronousQueue<>() : new ArrayBlockingQueue<>(settings.queueCapacity());
        workers = new ThreadPoolExecutor(
                settings.maxWorkers(), settings.maxWorkers(), 0L, TimeUnit.MILLISECONDS,
                queue, new CoordinatorThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
    }

    @Scheduled(fixedDelayString = "${app.tracking.provider-coordinator.tick-delay-ms:1000}")
    public void tick() {
        if (!accepting) {
            return;
        }
        Instant now = clock.instant();
        List<ClaimedProviderConnection> claimed = executions.claimDueConnections(
                leaseOwner, now, settings.leaseDuration(), settings.maxConnectionsPerTick());
        meters.counter("tracking.provider.coordinator.connections", "result", "claimed")
                .increment(claimed.size());
        for (ClaimedProviderConnection connection : claimed) {
            if (!localFlights.add(connection.id().value())) {
                releaseFailure(connection, FAILURE_SATURATED, now);
                continue;
            }
            try {
                workers.execute(() -> execute(connection.id().value()));
            } catch (RejectedExecutionException exception) {
                localFlights.remove(connection.id().value());
                meters.counter("tracking.provider.coordinator.worker", "result", "saturated")
                        .increment();
                releaseFailure(connection, FAILURE_SATURATED, now);
            }
        }
    }

    private void execute(UUID connectionId) {
        Instant startedAt = clock.instant();
        state.started();
        activeConnections.put(connectionId, nullType());
        boolean successful = false;
        Timer.Sample latency = Timer.start(meters);
        ClaimedProviderConnection connection = null;
        Semaphore quota = null;
        try {
            connection = executions.reloadActive(
                    new com.transportlogistics.app.tracking.application.provider.ProviderConnectionId(
                            connectionId), leaseOwner, startedAt).orElse(null);
            if (connection == null) {
                return;
            }
            activeConnections.put(connectionId, connection.providerType());
            meters.counter("tracking.provider.coordinator.connections", "result", "executed",
                    "provider", connection.providerType().value()).increment();
            quota = providerQuotas.computeIfAbsent(connection.providerType(), ignored ->
                    new Semaphore(settings.providerConcurrency()));
            if (!quota.tryAcquire()) {
                releaseFailure(connection, FAILURE_SATURATED, clock.instant());
                return;
            }
            successful = executeClaimed(connection);
        } finally {
            if (quota != null) {
                quota.release();
            }
            localFlights.remove(connectionId);
            activeConnections.remove(connectionId);
            state.finished(successful, clock.instant());
            latency.stop(meters.timer("tracking.provider.coordinator.job.latency",
                    "result", successful ? "success" : "failure"));
        }
    }

    private boolean executeClaimed(ClaimedProviderConnection originallyClaimed) {
        Instant now = clock.instant();
        ClaimedProviderConnection connection = executions.reloadActive(
                originallyClaimed.id(), leaseOwner, now).orElse(null);
        if (connection == null) {
            return false;
        }
        var adapter = adapters.find(connection.providerType()).orElse(null);
        if (adapter == null) {
            releaseFailure(connection, FAILURE_UNSUPPORTED, now);
            return false;
        }
        char[] secret = secrets.resolve(connection.credentialReference()).orElse(null);
        if (secret == null) {
            releaseFailure(connection, FAILURE_CREDENTIAL, now);
            return false;
        }
        Instant latestMessage = null;
        try {
            for (int batch = 0; batch < settings.maxBatchesPerJob(); batch++) {
                now = clock.instant();
                if (!executions.renewLease(
                        connection.id(), leaseOwner, now, settings.leaseDuration())) {
                    meters.counter("tracking.provider.coordinator.lease", "result", "contention")
                            .increment();
                    return false;
                }
                List<TrackingDeviceProviderBinding> page = executions.findDueActiveBindings(
                        connection.tenantId(), connection.id(), now, settings.maxDevicesPerFetch());
                if (page.isEmpty()) {
                    break;
                }
                List<ProviderDeviceCursor> cursors = page.stream().map(binding ->
                        new ProviderDeviceCursor(
                                binding.externalDeviceReference(),
                                new ProviderWatermark(binding.watermarkSourceTimestamp(),
                                        binding.watermarkMessageIdentity()))).toList();
                ProviderFetchRequest request = new ProviderFetchRequest(
                        connection.execution(), cursors,
                        Math.min(connection.pageSize(), settings.maxDevicesPerFetch()),
                        settings.responseByteLimit(), now.plus(settings.fetchDeadline()));
                var fetched = adapter.fetchPositions(request, secret);
                meters.counter("tracking.provider.coordinator.candidates", "result", "fetched",
                        "provider", connection.providerType().value()).increment(fetched.candidates().size());
                List<ProviderIngestionOutcome> outcomes = fetched.candidates().isEmpty()
                        ? List.of() : ingestion.ingest(
                                connection.id(), leaseOwner, fetched.candidates(), clock.instant());
                latestMessage = later(latestMessage, advanceSuccessfulWatermarks(
                        connection, page, fetched.candidates(), outcomes, fetched.nextWatermarks(), now));
            }
            Instant completedAt = clock.instant();
            boolean released = executions.releaseSuccess(
                    connection.id(), leaseOwner, completedAt,
                    completedAt.plusSeconds(connection.pollIntervalSeconds()), latestMessage);
            meters.counter("tracking.provider.coordinator.connections", "result", "success",
                    "provider", connection.providerType().value()).increment(released ? 1 : 0);
            return released;
        } catch (BusinessRuleException exception) {
            releaseFailure(connection, category(exception.code()), clock.instant());
            return false;
        } catch (RuntimeException exception) {
            releaseFailure(connection, FAILURE_PROVIDER, clock.instant());
            return false;
        } finally {
            Arrays.fill(secret, '\0');
        }
    }

    private Instant advanceSuccessfulWatermarks(
            ClaimedProviderConnection connection,
            List<TrackingDeviceProviderBinding> page,
            List<com.transportlogistics.app.tracking.application.provider.NormalizedPositionCandidate> candidates,
            List<ProviderIngestionOutcome> outcomes,
            Map<String, ProviderWatermark> providerWatermarks,
            Instant now) {
        Map<UUID, Advancement> advances = new HashMap<>();
        for (int index = 0; index < outcomes.size(); index++) {
            ProviderIngestionOutcome outcome = outcomes.get(index);
            String metricResult = outcome.result().name().toLowerCase(java.util.Locale.ROOT);
            meters.counter("tracking.provider.coordinator.candidates", "result", metricResult,
                    "provider", connection.providerType().value()).increment();
            if (outcome.result() == Result.REJECTED) {
                continue;
            }
            var candidate = candidates.get(index);
            ProviderWatermark proposed = providerWatermarks.get(candidate.externalDeviceReference());
            Instant source = proposed == null || proposed.sourceTimestamp() == null
                    ? outcome.sourceTimestamp() : proposed.sourceTimestamp();
            String identity = proposed == null
                    ? outcome.messageIdentity() : proposed.messageIdentity();
            Advancement current = advances.get(outcome.bindingId());
            if (current == null || source.isAfter(current.sourceTimestamp())) {
                advances.put(outcome.bindingId(),
                        new Advancement(outcome.bindingVersion(), source, identity));
            }
        }
        Instant next = now.plusSeconds(connection.pollIntervalSeconds());
        Instant latest = null;
        for (TrackingDeviceProviderBinding binding : page) {
            Advancement advancement = advances.get(binding.id());
            try {
                if (advancement == null) {
                    bindings.updateNextPoll(binding.tenantId(), binding.id(), binding.version(), next,
                            binding.updatedBy(), now);
                } else {
                    bindings.updateWatermark(
                            binding.tenantId(), binding.id(), advancement.version(),
                            advancement.sourceTimestamp(), advancement.messageIdentity(), next,
                            binding.updatedBy(), now);
                    latest = later(latest, advancement.sourceTimestamp());
                }
            } catch (BusinessRuleException exception) {
                meters.counter("tracking.provider.coordinator.candidates", "result", "cursor_conflict",
                        "provider", connection.providerType().value()).increment();
            }
        }
        return latest;
    }

    private void releaseFailure(
            ClaimedProviderConnection connection, String category, Instant now) {
        executions.releaseFailure(connection.id(), leaseOwner, now,
                now.plus(settings.failureBackoff()), category);
        meters.counter("tracking.provider.coordinator.connections", "result", "failure",
                "category", category, "provider", connection.providerType().value()).increment();
    }

    private static String category(String code) {
        if (code == null || code.isBlank()) {
            return FAILURE_PROVIDER;
        }
        String safe = code.toUpperCase(java.util.Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
        return safe.length() <= 40 ? safe : safe.substring(0, 40);
    }

    private static Instant later(Instant left, Instant right) {
        if (left == null) {
            return right;
        }
        return right != null && right.isAfter(left) ? right : left;
    }

    private static ProviderType nullType() {
        return ProviderType.of("PENDING");
    }

    @PreDestroy
    public void shutdown() {
        accepting = false;
        workers.shutdown();
        try {
            if (!workers.awaitTermination(settings.shutdownGrace().toMillis(), TimeUnit.MILLISECONDS)) {
                workers.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            workers.shutdownNow();
        }
        activeConnections.forEach((id, type) -> adapters.find(type).ifPresent(adapter ->
                adapter.close(new com.transportlogistics.app.tracking.application.provider.ProviderConnectionId(
                        id))));
        activeConnections.clear();
    }

    int workerPoolSize() {
        return workers.getPoolSize();
    }

    int workerQueueSize() {
        return workers.getQueue().size();
    }

    String leaseOwner() {
        return leaseOwner;
    }

    private record Advancement(long version, Instant sourceTimestamp, String messageIdentity) { }

    private static final class CoordinatorThreadFactory implements ThreadFactory {
        private final AtomicInteger sequence = new AtomicInteger();

        @Override
        public Thread newThread(Runnable task) {
            Thread thread = new Thread(task, "tracking-provider-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
