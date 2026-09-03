package com.transportlogistics.app.integration.application.service;

import com.transportlogistics.app.integration.IntegrationPlatformProbeEvent;
import com.transportlogistics.app.integration.domain.model.IntegrationAuditEvent;
import com.transportlogistics.app.integration.domain.model.IntegrationConfiguration;
import com.transportlogistics.app.integration.domain.model.IntegrationExchange;
import com.transportlogistics.app.integration.domain.model.IntegrationExchangeAttempt;
import com.transportlogistics.app.integration.domain.model.IntegrationMapping;
import com.transportlogistics.app.integration.ports.inbound.IntegrationExchangeUseCase;
import com.transportlogistics.app.integration.ports.inbound.IntegrationManagementUseCase;
import com.transportlogistics.app.integration.ports.outbound.IntegrationAttemptRepository;
import com.transportlogistics.app.integration.ports.outbound.IntegrationAuditRepository;
import com.transportlogistics.app.integration.ports.outbound.IntegrationConfigurationRepository;
import com.transportlogistics.app.integration.ports.outbound.IntegrationEndpointPort;
import com.transportlogistics.app.integration.ports.outbound.IntegrationEventPublisher;
import com.transportlogistics.app.integration.ports.outbound.IntegrationExchangeRepository;
import com.transportlogistics.app.integration.ports.outbound.IntegrationMappingRepository;
import com.transportlogistics.app.integration.ports.outbound.IntegrationPayloadPort;
import com.transportlogistics.app.integration.ports.outbound.IntegrationRateLimiter;
import com.transportlogistics.app.integration.ports.outbound.IntegrationTransaction;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.shared.domain.TooManyRequestsException;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class IntegrationService implements IntegrationManagementUseCase, IntegrationExchangeUseCase {
    static final int MAX_PAYLOAD_BYTES = 32 * 1024;
    static final int MAX_BATCH = 50;
    private static final String CONFIGURATION = "INTEGRATION_CONFIGURATION";
    private static final String EXCHANGE = "INTEGRATION_EXCHANGE";
    private static final String SYSTEM_ACTOR = "system:integration-exchange";

    private final IntegrationConfigurationRepository configurations;
    private final IntegrationMappingRepository mappings;
    private final IntegrationExchangeRepository exchanges;
    private final IntegrationAttemptRepository attempts;
    private final IntegrationAuditRepository audits;
    private final IntegrationEndpointPort endpoint;
    private final IntegrationPayloadPort payloads;
    private final IntegrationEventPublisher events;
    private final IntegrationRateLimiter rateLimiter;
    private final IntegrationTransaction transactions;
    private final Clock clock;

    public IntegrationService(IntegrationConfigurationRepository configurations,
                              IntegrationMappingRepository mappings,
                              IntegrationExchangeRepository exchanges,
                              IntegrationAttemptRepository attempts,
                              IntegrationAuditRepository audits,
                              IntegrationEndpointPort endpoint,
                              IntegrationPayloadPort payloads,
                              IntegrationEventPublisher events,
                              IntegrationRateLimiter rateLimiter,
                              IntegrationTransaction transactions,
                              Clock clock) {
        this.configurations = configurations;
        this.mappings = mappings;
        this.exchanges = exchanges;
        this.attempts = attempts;
        this.audits = audits;
        this.endpoint = endpoint;
        this.payloads = payloads;
        this.events = events;
        this.rateLimiter = rateLimiter;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public IntegrationView create(Context context, CreateCommand command) {
        validateContext(context);
        Objects.requireNonNull(command, "Integration create command is required");
        return transactions.execute(() -> {
            OffsetDateTime now = now();
            String normalizedName = IntegrationConfiguration.normalizeName(command.name());
            rejectDuplicateName(context.tenantId(), normalizedName, null);
            var draft = IntegrationConfiguration.draft(context.tenantId(), command.name(), command.type(),
                command.protocol(), command.direction(), command.endpointAlias(), command.credentialReference(),
                command.dataClassification(), now, context.actor());
            var persisted = configurations.save(draft);
            var mapping = createMapping(context, persisted.id(), 1, command.mapping(), now);
            var complete = configurations.save(persisted.withMapping(mapping.id(), now, context.actor()));
            audit(context, IntegrationAuditEvent.Action.CREATE, CONFIGURATION, complete.id(), null,
                mapping.definitionHash(), "SUCCESS");
            audit(context, IntegrationAuditEvent.Action.MAPPING_VERSION_CREATE, CONFIGURATION, complete.id(), null,
                mapping.definitionHash(), "SUCCESS");
            return new IntegrationView(complete, mapping);
        });
    }

    @Override
    public IntegrationView update(Context context, UUID id, UpdateCommand command) {
        validateContext(context);
        return transactions.execute(() -> {
            var existing = requiredConfiguration(context.tenantId(), id);
            requireVersion(existing.version(), command.version());
            rejectDuplicateName(context.tenantId(), IntegrationConfiguration.normalizeName(command.name()), id);
            var priorMapping = requiredMapping(context.tenantId(), existing.currentMappingId());
            int nextVersion = mappings.nextVersion(context.tenantId(), id, command.mapping().mappingKey());
            var nextMapping = createMapping(context, id, nextVersion, command.mapping(), now());
            mappings.supersede(context.tenantId(), priorMapping.id());
            String credentialReference = command.credentialReference() == null
                ? existing.credentialReference() : command.credentialReference();
            var updated = existing.update(command.name(), command.endpointAlias(), credentialReference,
                nextMapping.id(), now(), context.actor());
            var saved = configurations.save(updated);
            audit(context, IntegrationAuditEvent.Action.UPDATE, CONFIGURATION, id, priorMapping.definitionHash(),
                nextMapping.definitionHash(), "SUCCESS");
            if (!Objects.equals(existing.credentialReference(), credentialReference)) {
                audit(context, IntegrationAuditEvent.Action.CREDENTIAL_REFERENCE_CHANGE, CONFIGURATION, id,
                    null, null, "SUCCESS");
            }
            audit(context, IntegrationAuditEvent.Action.MAPPING_ACTIVATE, CONFIGURATION, id,
                priorMapping.definitionHash(), nextMapping.definitionHash(), "SUCCESS");
            return new IntegrationView(saved, nextMapping);
        });
    }

    @Override
    public IntegrationView get(Context context, UUID id) {
        validateContext(context);
        var configuration = requiredConfiguration(context.tenantId(), id);
        return new IntegrationView(configuration, requiredMapping(context.tenantId(), configuration.currentMappingId()));
    }

    @Override
    public PageResult<IntegrationView> list(Context context, int page, int size) {
        validateContext(context);
        int boundedSize = boundedSize(size);
        int boundedPage = Math.max(0, page);
        List<IntegrationView> content = configurations.list(context.tenantId(), boundedPage, boundedSize).stream()
            .map(configuration -> new IntegrationView(configuration,
                requiredMapping(context.tenantId(), configuration.currentMappingId())))
            .toList();
        long total = configurations.count(context.tenantId());
        return new PageResult<>(content, boundedPage, boundedSize, total, pages(total, boundedSize));
    }

    @Override
    public TestResult test(Context context, UUID id) {
        validateContext(context);
        return transactions.execute(() -> {
            OffsetDateTime testedAt = now();
            var configuration = requiredConfiguration(context.tenantId(), id);
            if (!rateLimiter.allow(context.tenantId(), id, context.actor(), testedAt)) {
                throw new TooManyRequestsException("INTEGRATION_RATE_LIMITED",
                    "Connection tests are limited to five per user and configuration per minute");
            }
            boolean success = true;
            String code = "INTEGRATION_TEST_SUCCEEDED";
            try {
                endpoint.probe(configuration.endpointAlias(), UUID.randomUUID());
            } catch (IntegrationEndpointPort.EndpointFailure failure) {
                success = false;
                code = failure.code();
            }
            var saved = configurations.save(configuration.tested(testedAt, success, context.actor()));
            audit(context, IntegrationAuditEvent.Action.TEST_CONNECTION, CONFIGURATION, id, null, null,
                success ? "SUCCESS" : code);
            return new TestResult(new IntegrationView(saved,
                requiredMapping(context.tenantId(), saved.currentMappingId())), success, code, testedAt);
        });
    }

    @Override
    public IntegrationView enable(Context context, UUID id, long version) {
        validateContext(context);
        return transactions.execute(() -> {
            var configuration = requiredConfiguration(context.tenantId(), id);
            requireVersion(configuration.version(), version);
            var mapping = requiredMapping(context.tenantId(), configuration.currentMappingId());
            var activated = configurations.save(configuration.activate(now(), context.actor()));
            UUID probeId = UUID.randomUUID();
            events.publishProbe(context.tenantId(), id, probeId, Math.max(1, now().toInstant().toEpochMilli()), now());
            audit(context, IntegrationAuditEvent.Action.ENABLE, CONFIGURATION, id, null,
                mapping.definitionHash(), "SUCCESS");
            return new IntegrationView(activated, mapping);
        });
    }

    @Override
    public IntegrationView disable(Context context, UUID id, long version) {
        validateContext(context);
        return transactions.execute(() -> {
            var configuration = requiredConfiguration(context.tenantId(), id);
            requireVersion(configuration.version(), version);
            var saved = configurations.save(configuration.disable(now(), context.actor()));
            audit(context, IntegrationAuditEvent.Action.DISABLE, CONFIGURATION, id, null, null, "SUCCESS");
            return new IntegrationView(saved, requiredMapping(context.tenantId(), saved.currentMappingId()));
        });
    }

    @Override
    public PageResult<ExchangeView> exchanges(Context context, UUID id, int page, int size) {
        validateContext(context);
        requiredConfiguration(context.tenantId(), id);
        int boundedSize = boundedSize(size);
        int boundedPage = Math.max(0, page);
        List<ExchangeView> content = exchanges.list(context.tenantId(), id, boundedPage, boundedSize).stream()
            .map(exchange -> new ExchangeView(exchange,
                attempts.findByExchange(context.tenantId(), exchange.id())))
            .toList();
        long total = exchanges.count(context.tenantId(), id);
        audit(context, IntegrationAuditEvent.Action.RECONCILIATION_VIEW, CONFIGURATION, id, null, null, "SUCCESS");
        return new PageResult<>(content, boundedPage, boundedSize, total, pages(total, boundedSize));
    }

    @Override
    public void acceptProbe(ProbeFact fact) {
        transactions.execute(() -> {
            validateProbe(fact);
            var configuration = requiredConfiguration(fact.tenantId(), fact.configurationId());
            if (configuration.lifecycle() != IntegrationConfiguration.Lifecycle.ACTIVE) {
                throw new BusinessRuleException("INTEGRATION_DISABLED", "Integration configuration is not active");
            }
            var mapping = requiredMapping(fact.tenantId(), configuration.currentMappingId());
            String canonicalPayload;
            try {
                canonicalPayload = payloads.serialize(mapping.apply(fact.payload()));
            } catch (IllegalArgumentException exception) {
                throw new BusinessRuleException("INTEGRATION_MAPPING_INVALID", exception.getMessage());
            }
            if (canonicalPayload.getBytes(StandardCharsets.UTF_8).length > MAX_PAYLOAD_BYTES) {
                throw new BusinessRuleException("INTEGRATION_PAYLOAD_INVALID", "Canonical payload exceeds 32 KiB");
            }
            OffsetDateTime now = now();
            var exchange = new IntegrationExchange(UUID.randomUUID(), fact.tenantId(), configuration.id(),
                fact.eventId(), fact.eventType(), mapping.id(), mapping.definitionHash(), canonicalPayload,
                payloads.hash(canonicalPayload), IntegrationExchange.Status.PENDING, 0, now, null, null, null,
                null, now, now, null, 0);
            var accepted = exchanges.saveIfAbsent(exchange);
            audit(new Context(fact.tenantId(), SYSTEM_ACTOR, fact.eventId().toString()),
                IntegrationAuditEvent.Action.DURABLE_FACT_ACCEPT, EXCHANGE, accepted.id(), null,
                accepted.payloadHash(), "SUCCESS");
            return null;
        });
    }

    @Override
    public void processDue(UUID tenantId) {
        exchanges.claimDue(tenantId, now(), MAX_BATCH).forEach(this::deliver);
    }

    private void deliver(IntegrationExchange exchange) {
        OffsetDateTime startedAt = now();
        try {
            var configuration = requiredConfiguration(exchange.tenantId(), exchange.configurationId());
            if (configuration.lifecycle() != IntegrationConfiguration.Lifecycle.ACTIVE) return;
            var result = endpoint.deliver(configuration.endpointAlias(), exchange.id(), exchange.canonicalPayload(),
                exchange.payloadHash());
            OffsetDateTime completedAt = now();
            transactions.execute(() -> {
                attempts.save(attempt(exchange, startedAt, completedAt,
                    IntegrationExchangeAttempt.Outcome.SUCCEEDED, null, result));
                exchanges.save(copy(exchange, IntegrationExchange.Status.SUCCEEDED, completedAt, null,
                    result.externalCorrelationId(), result.targetFilename(), completedAt));
                configurations.save(configuration.exchangeSucceeded(completedAt));
                audit(systemContext(exchange), IntegrationAuditEvent.Action.ATTEMPT, EXCHANGE, exchange.id(),
                    null, exchange.payloadHash(), "SUCCESS");
            });
        } catch (IntegrationEndpointPort.EndpointFailure failure) {
            OffsetDateTime completedAt = now();
            boolean retry = failure.retryable() && exchange.attemptCount() < IntegrationExchange.MAX_ATTEMPTS;
            transactions.execute(() -> {
                attempts.save(attempt(exchange, startedAt, completedAt, retry
                    ? IntegrationExchangeAttempt.Outcome.RETRYABLE_FAILURE
                    : IntegrationExchangeAttempt.Outcome.PERMANENT_FAILURE, failure.code(), null));
                OffsetDateTime next = retry
                    ? completedAt.plus(IntegrationExchange.backoffAfterAttempt(exchange.attemptCount())) : completedAt;
                exchanges.save(copy(exchange, retry ? IntegrationExchange.Status.RETRY_SCHEDULED
                    : IntegrationExchange.Status.FAILED, next, failure.code(), null, null,
                    retry ? null : completedAt));
                var configuration = requiredConfiguration(exchange.tenantId(), exchange.configurationId());
                configurations.save(configuration.exchangeFailed(retry,
                    "INTEGRATION_AUTH_FAILED".equals(failure.code()), completedAt));
                audit(systemContext(exchange), IntegrationAuditEvent.Action.ATTEMPT, EXCHANGE, exchange.id(),
                    null, exchange.payloadHash(), failure.code());
                if (!retry) {
                    audit(systemContext(exchange), IntegrationAuditEvent.Action.TERMINAL_FAILURE, EXCHANGE,
                        exchange.id(), null, exchange.payloadHash(), failure.code());
                }
            });
        }
    }

    private IntegrationExchange copy(IntegrationExchange exchange, IntegrationExchange.Status status,
                                     OffsetDateTime nextAttempt, String errorCode, String externalCorrelation,
                                     String targetFilename, OffsetDateTime completedAt) {
        return new IntegrationExchange(exchange.id(), exchange.tenantId(), exchange.configurationId(),
            exchange.sourceEventId(), exchange.sourceEventType(), exchange.mappingVersionId(),
            exchange.mappingDefinitionHash(), exchange.canonicalPayload(), exchange.payloadHash(), status,
            exchange.attemptCount(), nextAttempt, null, externalCorrelation, targetFilename, errorCode,
            exchange.createdAt(), now(), completedAt, exchange.version());
    }

    private IntegrationExchangeAttempt attempt(IntegrationExchange exchange, OffsetDateTime started,
                                               OffsetDateTime completed, IntegrationExchangeAttempt.Outcome outcome,
                                               String code, IntegrationEndpointPort.DeliveryResult result) {
        return new IntegrationExchangeAttempt(UUID.randomUUID(), exchange.tenantId(), exchange.id(),
            exchange.attemptCount(), started, completed, Math.max(0, Duration.between(started, completed).toMillis()),
            outcome, code, result == null ? null : result.externalCorrelationId(),
            result == null ? null : result.targetFilename());
    }

    private IntegrationMapping createMapping(Context context, UUID configurationId, int mappingVersion,
                                             MappingCommand command, OffsetDateTime now) {
        Objects.requireNonNull(command, "Mapping is required");
        return mappings.save(IntegrationMapping.active(context.tenantId(), configurationId,
            command.mappingKey(), mappingVersion, command.sourceContract(), command.sourceVersion(),
            command.targetSchema(), command.targetVersion(), command.rules(), now, context.actor()));
    }

    private IntegrationConfiguration requiredConfiguration(UUID tenantId, UUID id) {
        return configurations.findConfiguration(tenantId, id).orElseThrow(() ->
            new NotFoundException("INTEGRATION_NOT_FOUND", "Integration configuration was not found"));
    }

    private IntegrationMapping requiredMapping(UUID tenantId, UUID id) {
        if (id == null) throw new BusinessRuleException("INTEGRATION_MAPPING_INVALID", "Mapping is absent");
        return mappings.findMapping(tenantId, id).orElseThrow(() ->
            new BusinessRuleException("INTEGRATION_MAPPING_INVALID", "Mapping was not found"));
    }

    private void rejectDuplicateName(UUID tenantId, String normalizedName, UUID excludingId) {
        if (configurations.existsByNormalizedName(tenantId, normalizedName, excludingId)) {
            throw new ConflictException("INTEGRATION_DUPLICATE", "Integration name already exists for this Tenant");
        }
    }

    private void requireVersion(long actual, long expected) {
        if (actual != expected) {
            throw new ConflictException("INTEGRATION_CONFLICT", "Integration configuration changed; reload and retry");
        }
    }

    private void validateProbe(ProbeFact fact) {
        if (!IntegrationPlatformProbeEvent.EVENT_TYPE.equals(fact.eventType()) || fact.version() != 1
                || !CONFIGURATION.equals(fact.aggregateType()) || fact.eventId() == null
                || fact.tenantId() == null || fact.configurationId() == null || fact.occurredAt() == null) {
            throw new BusinessRuleException("INTEGRATION_PAYLOAD_INVALID", "Unsupported platform probe envelope");
        }
        if (!fact.payload().keySet().equals(java.util.Set.of("probeId", "probeType", "sequence"))
                || !"CONTROLLED_SANDBOX".equals(fact.payload().get("probeType"))) {
            throw new BusinessRuleException("INTEGRATION_PAYLOAD_INVALID", "Platform probe payload is invalid");
        }
    }

    private void audit(Context context, IntegrationAuditEvent.Action action, String targetType, UUID targetId,
                       String beforeHash, String afterHash, String code) {
        audits.append(new IntegrationAuditEvent(UUID.randomUUID(), context.tenantId(), context.actor(), action,
            targetType, targetId, "SUCCESS".equals(code) ? IntegrationAuditEvent.Outcome.SUCCESS
            : IntegrationAuditEvent.Outcome.FAILURE, "SUCCESS".equals(code) ? null : code,
            beforeHash, afterHash, context.correlationId(), now()));
    }

    private Context systemContext(IntegrationExchange exchange) {
        return new Context(exchange.tenantId(), SYSTEM_ACTOR, exchange.sourceEventId().toString());
    }

    private void validateContext(Context context) {
        if (context == null || context.tenantId() == null || context.actor() == null || context.actor().isBlank()) {
            throw new BusinessRuleException("INTEGRATION_CONFIGURATION_INVALID", "Active Tenant and actor are required");
        }
    }

    private int boundedSize(int size) { return size <= 0 ? 20 : Math.min(100, size); }
    private int pages(long total, int size) { return total == 0 ? 0 : (int) ((total + size - 1) / size); }
    private OffsetDateTime now() { return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC); }
}
