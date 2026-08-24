package com.transportlogistics.app.offlinesync.infrastructure.testing;

import com.transportlogistics.app.offlinesync.domain.model.OfflineHandlerOutcome;
import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncRetryableException;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/** Deterministic operation-scoped outcomes available only through the e2e profile. */
public final class E2eOfflineSyncControl {
    private final Map<UUID, Rule> rules = new ConcurrentHashMap<>();

    public void configure(UUID operationId, Mode mode, int remainingAttempts) {
        if (remainingAttempts < 1) {
            throw new IllegalArgumentException("remainingAttempts must be positive");
        }
        rules.put(operationId, new Rule(mode, remainingAttempts, new CompletableFuture<>()));
    }

    public OfflineHandlerOutcome beforeApply(UUID operationId) {
        Rule rule = rules.get(operationId);
        if (rule == null) {
            return null;
        }
        if (rule.mode == Mode.BLOCK) {
            try {
                rule.release.get(20, TimeUnit.SECONDS);
            } catch (Exception exception) {
                throw new OfflineSyncRetryableException("E2E blocked operation timed out", exception);
            }
        }
        if (rule.consume()) {
            rules.remove(operationId, rule);
        }
        return switch (rule.mode) {
            case APPLIED, BLOCK -> null;
            case REJECTED -> OfflineHandlerOutcome.rejected("OFFLINE_SYNC_PAYLOAD_INVALID", "Deterministic E2E rejection");
            case CONFLICT -> OfflineHandlerOutcome.conflict("OFFLINE_SYNC_CONFLICT", "Deterministic E2E business conflict");
            case RETRYABLE -> throw new OfflineSyncRetryableException("Deterministic E2E transient failure");
        };
    }

    public void release(UUID operationId) {
        Rule rule = rules.get(operationId);
        if (rule != null) {
            rule.release.complete(null);
        }
    }

    public void reset() {
        rules.values().forEach(rule -> rule.release.complete(null));
        rules.clear();
    }

    public enum Mode { APPLIED, REJECTED, CONFLICT, RETRYABLE, BLOCK }

    private static final class Rule {
        private final Mode mode;
        private int remainingAttempts;
        private final CompletableFuture<Void> release;

        private Rule(Mode mode, int remainingAttempts, CompletableFuture<Void> release) {
            this.mode = mode;
            this.remainingAttempts = remainingAttempts;
            this.release = release;
        }

        private synchronized boolean consume() {
            remainingAttempts--;
            return remainingAttempts == 0;
        }
    }
}
