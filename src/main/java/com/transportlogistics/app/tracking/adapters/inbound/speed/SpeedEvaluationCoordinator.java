package com.transportlogistics.app.tracking.adapters.inbound.speed;

import com.transportlogistics.app.tracking.adapters.configuration.SpeedEvaluatorSettings;
import com.transportlogistics.app.tracking.domain.speed.SpeedEvaluationJob;
import com.transportlogistics.app.tracking.domain.speed.SpeedMonitoringException;
import com.transportlogistics.app.tracking.ports.inbound.SpeedEvaluationJobUseCase;
import com.transportlogistics.app.tracking.ports.outbound.SpeedEvaluationJobRepositoryPort;
import com.transportlogistics.app.tenancy.TenantContextExecutor;
import com.transportlogistics.app.tenancy.TenantExecutionContext;
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
@ConditionalOnProperty(name = "app.tracking.speed-evaluator.enabled", havingValue = "true")
public final class SpeedEvaluationCoordinator {
    private static final UUID SYSTEM_ACTOR =
            UUID.fromString("00000000-0000-0000-0000-000000000050");
    private final SpeedEvaluationJobRepositoryPort jobs;
    private final SpeedEvaluationJobUseCase evaluator;
    private final SpeedEvaluatorSettings settings;
    private final MeterRegistry meters;
    private final Clock clock;
    private final TenantContextExecutor tenantContexts;
    private final String leaseOwner;
    private final ThreadPoolExecutor workers;
    private final java.util.Set<JobKey> localJobs = ConcurrentHashMap.newKeySet();
    private volatile boolean accepting = true;

    @Autowired
    public SpeedEvaluationCoordinator(
            SpeedEvaluationJobRepositoryPort jobs, SpeedEvaluationJobUseCase evaluator,
            SpeedEvaluatorSettings settings, MeterRegistry meters, Clock clock,
            TenantContextExecutor tenantContexts) {
        this(jobs, evaluator, settings, meters, clock, tenantContexts,
                "speed-" + UUID.randomUUID());
    }

    SpeedEvaluationCoordinator(
            SpeedEvaluationJobRepositoryPort jobs, SpeedEvaluationJobUseCase evaluator,
            SpeedEvaluatorSettings settings, MeterRegistry meters, Clock clock,
            TenantContextExecutor tenantContexts, String leaseOwner) {
        this.jobs = jobs;
        this.evaluator = evaluator;
        this.settings = settings;
        this.meters = meters;
        this.clock = clock;
        this.tenantContexts = tenantContexts;
        this.leaseOwner = leaseOwner;
        BlockingQueue<Runnable> queue = settings.queueCapacity() == 0
                ? new SynchronousQueue<>() : new ArrayBlockingQueue<>(settings.queueCapacity());
        workers = new ThreadPoolExecutor(settings.workerCount(), settings.workerCount(), 0,
                TimeUnit.MILLISECONDS, queue, new WorkerThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
        meters.gauge("tracking.speed.evaluator.queue", workers,
                executor -> executor.getQueue().size());
        meters.gauge("tracking.speed.evaluator.active", workers,
                ThreadPoolExecutor::getActiveCount);
    }

    @Scheduled(fixedDelayString = "${app.tracking.speed-evaluator.tick-delay-ms:1000}")
    public void tick() {
        if (!accepting) return;
        Instant now = clock.instant();
        List<SpeedEvaluationJob> claimed = jobs.claimDue(
                leaseOwner, now, now.plus(settings.leaseDuration()), settings.claimSize());
        meters.counter("tracking.speed.evaluator.jobs", "result", "claimed")
                .increment(claimed.size());
        for (SpeedEvaluationJob job : claimed) submit(job, now);
    }

    private void submit(SpeedEvaluationJob job, Instant now) {
        JobKey key = new JobKey(job.tenantId(), job.positionId());
        if (!localJobs.add(key)) {
            jobs.release(job.tenantId(), job.positionId(), leaseOwner, now);
            return;
        }
        try {
            workers.execute(() -> execute(job));
        } catch (RejectedExecutionException exception) {
            localJobs.remove(key);
            jobs.release(job.tenantId(), job.positionId(), leaseOwner, now);
            meters.counter("tracking.speed.evaluator.jobs", "result", "saturated").increment();
        }
    }

    private void execute(SpeedEvaluationJob job) {
        String result = "completed";
        try {
            Instant now = clock.instant();
            if (!jobs.renew(job.tenantId(), job.positionId(), leaseOwner, now,
                    now.plus(settings.leaseDuration()))) return;
            tenantContexts.within(new TenantExecutionContext(job.tenantId(), SYSTEM_ACTOR,
                    "tracking-speed-evaluator", leaseOwner),
                    () -> evaluator.process(job, leaseOwner, now));
        } catch (SpeedMonitoringException exception) {
            result = "terminal";
            jobs.fail(job.tenantId(), job.positionId(), leaseOwner,
                    safeErrorCode(exception.code()), clock.instant());
        } catch (RuntimeException exception) {
            result = retryOrFail(job);
        } finally {
            localJobs.remove(new JobKey(job.tenantId(), job.positionId()));
            meters.counter("tracking.speed.evaluator.jobs", "result", result).increment();
        }
    }

    private String retryOrFail(SpeedEvaluationJob job) {
        Instant now = clock.instant();
        if (job.attemptCount() >= settings.maximumAttempts()) {
            jobs.fail(job.tenantId(), job.positionId(), leaseOwner,
                    "SPEED_EVALUATION_RETRY_EXHAUSTED", now);
            return "terminal";
        }
        jobs.retry(job.tenantId(), job.positionId(), leaseOwner, now,
                now.plus(settings.retryBackoff()));
        return "retry";
    }

    private static String safeErrorCode(String code) {
        return code == null || !code.matches("[A-Z0-9_]{1,120}")
                ? "SPEED_EVALUATION_REJECTED" : code;
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
    }

    private static final class WorkerThreadFactory implements ThreadFactory {
        private final AtomicInteger sequence = new AtomicInteger();

        @Override
        public Thread newThread(Runnable task) {
            Thread thread = new Thread(task,
                    "tracking-speed-evaluator-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }

    private record JobKey(UUID tenantId, UUID positionId) {
    }
}
