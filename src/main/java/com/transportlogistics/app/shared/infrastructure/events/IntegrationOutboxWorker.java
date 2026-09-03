package com.transportlogistics.app.shared.infrastructure.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.shared.DurableEventHandler;
import com.transportlogistics.app.shared.DurableEventWorker;
import com.transportlogistics.app.shared.PermanentEventFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class IntegrationOutboxWorker implements DurableEventWorker {
    static final int BATCH_SIZE = 50;
    static final int MAX_ATTEMPTS = 5;
    private static final Logger log = LoggerFactory.getLogger(IntegrationOutboxWorker.class);
    private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {};

    private final IntegrationOutboxTransactions transactions;
    private final ObjectMapper objectMapper;
    private final Map<String, DurableEventHandler> handlers;
    private final Clock clock;

    IntegrationOutboxWorker(IntegrationOutboxTransactions transactions, ObjectMapper objectMapper,
                            List<DurableEventHandler> handlers, Clock clock) {
        this.transactions = transactions;
        this.objectMapper = objectMapper;
        this.handlers = handlers.stream().collect(Collectors.toUnmodifiableMap(
            DurableEventHandler::consumerName, Function.identity()));
        this.clock = clock;
    }

    @Override
    public void processDue() {
        OffsetDateTime claimTime = now();
        for (ClaimedOutboxEvent event : transactions.claim(claimTime, BATCH_SIZE)) {
            process(event);
        }
    }

    private void process(ClaimedOutboxEvent event) {
        try {
            if (event.eventVersion() != 1) {
                throw new PermanentEventFailureException("UNSUPPORTED_VERSION",
                    "Unsupported durable event version");
            }
            DurableEventHandler handler = handlers.get(event.consumerName());
            if (handler == null) {
                throw new PermanentEventFailureException("HANDLER_NOT_FOUND", "Durable event handler is absent");
            }
            Map<String, Object> payload = objectMapper.readValue(event.payload(), PAYLOAD_TYPE);
            handler.handle(new StoredDurableEvent(event.eventId(), event.eventType(), event.tenantId(),
                event.occurredAt(), event.eventVersion(), event.aggregateType(), event.aggregateId(), payload,
                event.consumerName()));
            transactions.published(event.id(), now());
            log.info("Durable event published: eventId={}, eventType={}, version={}, aggregateType={}, "
                    + "aggregateId={}, tenantId={}, attempt={}", event.eventId(), event.eventType(),
                event.eventVersion(), event.aggregateType(), event.aggregateId(), event.tenantId(),
                event.attemptCount());
        } catch (JsonProcessingException exception) {
            park(event, "INVALID_PAYLOAD", false);
        } catch (PermanentEventFailureException exception) {
            park(event, exception.errorCode(), "UNSUPPORTED_VERSION".equals(exception.errorCode()));
        } catch (RuntimeException exception) {
            retry(event, exception);
        }
    }

    private void retry(ClaimedOutboxEvent event, RuntimeException failure) {
        OffsetDateTime failedAt = now();
        String errorCode = safeCode(failure);
        if (event.attemptCount() >= MAX_ATTEMPTS) {
            transactions.failed(event.id(), failedAt, "RETRY_EXHAUSTED_" + errorCode, false);
            log.error("Durable event exhausted retries: eventId={}, eventType={}, tenantId={}, attempts={}, code={}",
                event.eventId(), event.eventType(), event.tenantId(), event.attemptCount(), errorCode);
            return;
        }
        transactions.retry(event.id(), failedAt, failedAt.plus(backoff(event.attemptCount())), errorCode);
        log.warn("Durable event scheduled for retry: eventId={}, eventType={}, tenantId={}, attempt={}, code={}",
            event.eventId(), event.eventType(), event.tenantId(), event.attemptCount(), errorCode);
    }

    private void park(ClaimedOutboxEvent event, String code, boolean unsupported) {
        transactions.failed(event.id(), now(), code, unsupported);
        log.error("Durable event parked: eventId={}, eventType={}, tenantId={}, version={}, code={}",
            event.eventId(), event.eventType(), event.tenantId(), event.eventVersion(), code);
    }

    static Duration backoff(int attempt) {
        int exponent = Math.min(5, Math.max(0, attempt - 1));
        return Duration.ofSeconds(Math.min(900L, 30L << exponent));
    }

    private String safeCode(RuntimeException exception) {
        String name = exception.getClass().getSimpleName().replaceAll("[^A-Za-z0-9]", "_").toUpperCase();
        return name.length() <= 60 ? name : name.substring(0, 60);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
