package com.transportlogistics.app.tracking.adapters.inbound.geofence;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.tracking.adapters.configuration.GeofenceEvaluatorSettings;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceEvaluationJob;
import com.transportlogistics.app.tracking.ports.inbound.GeofenceEvaluationUseCase;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceEvaluationJobRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.GeofencePositionRepositoryPort;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.tracking.geofence-evaluator.enabled", havingValue = "true")
public final class GeofenceEvaluationCoordinator {
    private final GeofenceEvaluationJobRepositoryPort jobs;
    private final GeofencePositionRepositoryPort positions;
    private final GeofenceEvaluationUseCase evaluator;
    private final GeofenceEvaluatorSettings settings;
    private final MeterRegistry meters;
    private final Clock clock;
    private final String leaseOwner;
    private final ThreadPoolExecutor workers;
    private final java.util.Set<JobKey> localJobs = ConcurrentHashMap.newKeySet();
    private volatile boolean accepting = true;

    @Autowired
    public GeofenceEvaluationCoordinator(
            GeofenceEvaluationJobRepositoryPort jobs, GeofencePositionRepositoryPort positions,
            GeofenceEvaluationUseCase evaluator, GeofenceEvaluatorSettings settings,
            MeterRegistry meters, Clock clock) {
        this(jobs, positions, evaluator, settings, meters, clock,
                "geofence-" + UUID.randomUUID());
    }

    GeofenceEvaluationCoordinator(
            GeofenceEvaluationJobRepositoryPort jobs, GeofencePositionRepositoryPort positions,
            GeofenceEvaluationUseCase evaluator, GeofenceEvaluatorSettings settings,
            MeterRegistry meters, Clock clock, String leaseOwner) {
        this.jobs = jobs;
        this.positions = positions;
        this.evaluator = evaluator;
        this.settings = settings;
        this.meters = meters;
        this.clock = clock;
        this.leaseOwner = leaseOwner;
        BlockingQueue<Runnable> queue = settings.queueCapacity() == 0
                ? new SynchronousQueue<>() : new ArrayBlockingQueue<>(settings.queueCapacity());
        workers = new ThreadPoolExecutor(settings.workerCount(), settings.workerCount(), 0,
                TimeUnit.MILLISECONDS, queue, new WorkerThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
        meters.gauge("tracking.geofence.evaluator.queue", workers,
                executor -> executor.getQueue().size());
        meters.gauge("tracking.geofence.evaluator.active", workers,
                ThreadPoolExecutor::getActiveCount);
    }

    @Scheduled(fixedDelayString = "${app.tracking.geofence-evaluator.tick-delay-ms:1000}")
    public void tick() {
        if (!accepting) {
            return;
        }
        Instant now = clock.instant();
        var backlog = jobs.backlog(now);
        meters.summary("tracking.geofence.evaluator.backlog").record(backlog.queued());
        meters.summary("tracking.geofence.evaluator.claimed").record(backlog.claimed());
        if (backlog.oldestDueAt() != null) {
            meters.summary("tracking.geofence.evaluator.oldest.due.seconds").record(
                    Math.max(0, java.time.Duration.between(
                            backlog.oldestDueAt(), now).toSeconds()));
        }
        List<GeofenceEvaluationJob> claimed = jobs.claimDue(
                leaseOwner, now, now.plus(settings.leaseDuration()), settings.claimSize());
        meters.counter("tracking.geofence.evaluator.jobs", "result", "claimed")
                .increment(claimed.size());
        for (GeofenceEvaluationJob job : claimed) {
            JobKey key = new JobKey(job.tenantId(), job.positionId());
            if (!localJobs.add(key)) {
                jobs.release(job.tenantId(), job.positionId(), leaseOwner, now);
                continue;
            }
            try {
                workers.execute(() -> execute(job));
            } catch (RejectedExecutionException exception) {
                localJobs.remove(key);
                jobs.release(job.tenantId(), job.positionId(), leaseOwner, now);
                meters.counter("tracking.geofence.evaluator.jobs", "result", "saturated")
                        .increment();
            }
        }
    }

    private void execute(GeofenceEvaluationJob job) {
        String result = "completed";
        try {
            Instant now = clock.instant();
            if (!jobs.renew(job.tenantId(), job.positionId(), leaseOwner, now,
                    now.plus(settings.leaseDuration()))) {
                return;
            }
            var position = positions.find(job.tenantId(), job.positionId());
            if (position.isPresent()) {
                evaluator.evaluate(position.get(), now).stream()
                        .flatMap(evaluation -> evaluation.transition().stream())
                        .forEach(transition -> meters.summary(
                                "tracking.geofence.evaluator.transition.latency.seconds").record(
                                Math.max(0, java.time.Duration.between(
                                        transition.sourceTimestamp(), clock.instant()).toSeconds())));
            }
            jobs.complete(job.tenantId(), job.positionId(), leaseOwner, clock.instant());
        } catch (BusinessRuleException exception) {
            if ("GEOFENCE_ACTIVE_LIMIT_EXCEEDED".equals(exception.code())) {
                result = "terminal";
                jobs.fail(job.tenantId(), job.positionId(), leaseOwner, clock.instant());
            } else {
                result = "retry";
                retry(job);
            }
        } catch (RuntimeException exception) {
            result = "retry";
            retry(job);
        } finally {
            localJobs.remove(new JobKey(job.tenantId(), job.positionId()));
            meters.counter("tracking.geofence.evaluator.jobs", "result", result).increment();
        }
    }

    private void retry(GeofenceEvaluationJob job) {
        Instant now = clock.instant();
        jobs.retry(job.tenantId(), job.positionId(), leaseOwner, now,
                now.plus(settings.retryBackoff()));
    }

    @PreDestroy
    public void shutdown() {
        accepting = false;
        workers.shutdown();
        try {
            if (!workers.awaitTermination(settings.shutdownGrace().toMillis(),
                    TimeUnit.MILLISECONDS)) {
                workers.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            workers.shutdownNow();
        }
    }

    private static final class WorkerThreadFactory implements ThreadFactory {
        private final AtomicInteger sequence = new AtomicInteger();

        @Override
        public Thread newThread(Runnable task) {
            Thread thread = new Thread(task,
                    "tracking-geofence-evaluator-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }

    private record JobKey(UUID tenantId, UUID positionId) {
    }
}
