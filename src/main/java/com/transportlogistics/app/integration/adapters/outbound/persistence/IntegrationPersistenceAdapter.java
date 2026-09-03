package com.transportlogistics.app.integration.adapters.outbound.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.integration.domain.model.IntegrationAuditEvent;
import com.transportlogistics.app.integration.domain.model.IntegrationConfiguration;
import com.transportlogistics.app.integration.domain.model.IntegrationExchange;
import com.transportlogistics.app.integration.domain.model.IntegrationExchangeAttempt;
import com.transportlogistics.app.integration.domain.model.IntegrationMapping;
import com.transportlogistics.app.integration.ports.outbound.IntegrationAttemptRepository;
import com.transportlogistics.app.integration.ports.outbound.IntegrationAuditRepository;
import com.transportlogistics.app.integration.ports.outbound.IntegrationConfigurationRepository;
import com.transportlogistics.app.integration.ports.outbound.IntegrationExchangeRepository;
import com.transportlogistics.app.integration.ports.outbound.IntegrationMappingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class IntegrationPersistenceAdapter implements IntegrationConfigurationRepository, IntegrationMappingRepository,
        IntegrationExchangeRepository, IntegrationAttemptRepository, IntegrationAuditRepository {
    private static final TypeReference<List<IntegrationMapping.Rule>> RULES_TYPE = new TypeReference<>() {};
    private final IntegrationConfigurationJpaRepository configurationJpa;
    private final IntegrationMappingJpaRepository mappingJpa;
    private final IntegrationExchangeJpaRepository exchangeJpa;
    private final IntegrationExchangeAttemptJpaRepository attemptJpa;
    private final IntegrationAuditEventJpaRepository auditJpa;
    private final ObjectMapper json;

    IntegrationPersistenceAdapter(IntegrationConfigurationJpaRepository configurationJpa,
                                  IntegrationMappingJpaRepository mappingJpa,
                                  IntegrationExchangeJpaRepository exchangeJpa,
                                  IntegrationExchangeAttemptJpaRepository attemptJpa,
                                  IntegrationAuditEventJpaRepository auditJpa,
                                  ObjectMapper json) {
        this.configurationJpa = configurationJpa;
        this.mappingJpa = mappingJpa;
        this.exchangeJpa = exchangeJpa;
        this.attemptJpa = attemptJpa;
        this.auditJpa = auditJpa;
        this.json = json;
    }

    @Override
    public IntegrationConfiguration save(IntegrationConfiguration configuration) {
        return configurationToDomain(configurationJpa.saveAndFlush(configurationToEntity(configuration)));
    }

    @Override
    public Optional<IntegrationConfiguration> findConfiguration(UUID tenantId, UUID id) {
        return configurationJpa.findByTenantIdAndId(tenantId, id).map(this::configurationToDomain);
    }

    @Override
    public boolean existsByNormalizedName(UUID tenantId, String normalizedName, UUID excludingId) {
        return excludingId == null ? configurationJpa.existsByTenantIdAndNormalizedName(tenantId, normalizedName)
            : configurationJpa.existsByTenantIdAndNormalizedNameAndIdNot(tenantId, normalizedName, excludingId);
    }

    @Override
    public List<IntegrationConfiguration> list(UUID tenantId, int page, int size) {
        return configurationJpa.findByTenantIdOrderByNormalizedName(tenantId, PageRequest.of(page, size)).stream()
            .map(this::configurationToDomain).toList();
    }

    @Override public long count(UUID tenantId) { return configurationJpa.countByTenantId(tenantId); }

    @Override
    public IntegrationMapping save(IntegrationMapping mapping) {
        return mappingToDomain(mappingJpa.saveAndFlush(mappingToEntity(mapping)));
    }

    @Override
    public Optional<IntegrationMapping> findMapping(UUID tenantId, UUID id) {
        return mappingJpa.findByTenantIdAndId(tenantId, id).map(this::mappingToDomain);
    }

    @Override
    public int nextVersion(UUID tenantId, UUID configurationId, String mappingKey) {
        return mappingJpa.maxVersion(tenantId, configurationId, mappingKey) + 1;
    }

    @Override
    public void supersede(UUID tenantId, UUID mappingId) {
        mappingJpa.findByTenantIdAndId(tenantId, mappingId).ifPresent(entity -> {
            entity.setLifecycle(IntegrationMapping.Lifecycle.SUPERSEDED);
            mappingJpa.save(entity);
        });
    }

    @Override
    public IntegrationExchange save(IntegrationExchange exchange) {
        return exchangeToDomain(exchangeJpa.saveAndFlush(exchangeToEntity(exchange)));
    }

    @Override
    public synchronized IntegrationExchange saveIfAbsent(IntegrationExchange exchange) {
        var existing = exchangeJpa.findByTenantIdAndConfigurationIdAndSourceEventIdAndMappingVersionId(
            exchange.tenantId(), exchange.configurationId(), exchange.sourceEventId(), exchange.mappingVersionId());
        return existing.map(this::exchangeToDomain).orElseGet(() -> save(exchange));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<IntegrationExchange> claimDue(UUID tenantId, OffsetDateTime now, int batchSize) {
        exchangeJpa.findExpiredExhausted(tenantId, now, IntegrationExchange.Status.IN_PROGRESS,
            IntegrationExchange.MAX_ATTEMPTS, PageRequest.of(0, batchSize)).forEach(entity -> entity.exhaust(now));
        var claimed = exchangeJpa.findClaimable(tenantId, now,
            EnumSet.of(IntegrationExchange.Status.PENDING, IntegrationExchange.Status.RETRY_SCHEDULED),
            IntegrationExchange.Status.IN_PROGRESS, IntegrationConfiguration.Lifecycle.ACTIVE,
            IntegrationExchange.MAX_ATTEMPTS, PageRequest.of(0, Math.min(50, batchSize)));
        claimed.forEach(entity -> entity.claim(now));
        exchangeJpa.flush();
        return claimed.stream().map(this::exchangeToDomain).toList();
    }

    @Override
    public List<IntegrationExchange> list(UUID tenantId, UUID configurationId, int page, int size) {
        return exchangeJpa.findByTenantIdAndConfigurationIdOrderByCreatedAtDesc(tenantId, configurationId,
            PageRequest.of(page, size)).stream().map(this::exchangeToDomain).toList();
    }

    @Override
    public long count(UUID tenantId, UUID configurationId) {
        return exchangeJpa.countByTenantIdAndConfigurationId(tenantId, configurationId);
    }

    @Override
    public Optional<IntegrationExchange> findExchange(UUID tenantId, UUID id) {
        return exchangeJpa.findByTenantIdAndId(tenantId, id).map(this::exchangeToDomain);
    }

    @Override
    public IntegrationExchangeAttempt save(IntegrationExchangeAttempt attempt) {
        return attemptToDomain(attemptJpa.saveAndFlush(attemptToEntity(attempt)));
    }

    @Override
    public List<IntegrationExchangeAttempt> findByExchange(UUID tenantId, UUID exchangeId) {
        return attemptJpa.findByTenantIdAndExchangeIdOrderByAttemptNumber(tenantId, exchangeId).stream()
            .map(this::attemptToDomain).toList();
    }

    @Override
    public void append(IntegrationAuditEvent event) {
        auditJpa.save(auditToEntity(event));
    }

    private IntegrationConfigurationEntity configurationToEntity(IntegrationConfiguration source) {
        var target = new IntegrationConfigurationEntity();
        target.setId(source.id());
        target.setTenantId(source.tenantId());
        target.setName(source.name());
        target.setNormalizedName(source.normalizedName());
        target.setType(source.type());
        target.setProtocol(source.protocol());
        target.setDirection(source.direction());
        target.setEndpointAlias(source.endpointAlias());
        target.setCredentialReference(source.credentialReference());
        target.setCurrentMappingId(source.currentMappingId());
        target.setDataClassification(source.dataClassification());
        target.setRetryPolicy(source.retryPolicy());
        target.setLifecycle(source.lifecycle());
        target.setHealth(source.health());
        target.setLastTestedAt(source.lastTestedAt());
        target.setLastTestedVersion(source.lastTestedVersion());
        target.setLastSuccessfulExchangeAt(source.lastSuccessfulExchangeAt());
        target.setVersion(source.version());
        target.setCreatedAt(source.createdAt());
        target.setCreatedBy(source.createdBy());
        target.setUpdatedAt(source.updatedAt());
        target.setUpdatedBy(source.updatedBy());
        return target;
    }

    private IntegrationConfiguration configurationToDomain(IntegrationConfigurationEntity source) {
        return new IntegrationConfiguration(source.getId(), source.getTenantId(), source.getName(),
            source.getNormalizedName(), source.getType(), source.getProtocol(), source.getDirection(),
            source.getEndpointAlias(), source.getCredentialReference(), source.getCurrentMappingId(),
            source.getDataClassification(), source.getRetryPolicy(), source.getLifecycle(), source.getHealth(),
            source.getLastTestedAt(), source.getLastTestedVersion(), source.getLastSuccessfulExchangeAt(),
            source.getVersion(), source.getCreatedAt(), source.getCreatedBy(), source.getUpdatedAt(),
            source.getUpdatedBy());
    }

    private IntegrationMappingEntity mappingToEntity(IntegrationMapping source) {
        var target = new IntegrationMappingEntity();
        target.setId(source.id());
        target.setTenantId(source.tenantId());
        target.setConfigurationId(source.configurationId());
        target.setMappingKey(source.mappingKey());
        target.setMappingVersion(source.mappingVersion());
        target.setSourceContract(source.sourceContract());
        target.setSourceVersion(source.sourceVersion());
        target.setTargetSchema(source.targetSchema());
        target.setTargetVersion(source.targetVersion());
        target.setRules(write(source.rules()));
        target.setDefinitionHash(source.definitionHash());
        target.setLifecycle(source.lifecycle());
        target.setCreatedAt(source.createdAt());
        target.setCreatedBy(source.createdBy());
        return target;
    }

    private IntegrationMapping mappingToDomain(IntegrationMappingEntity source) {
        return new IntegrationMapping(source.getId(), source.getTenantId(), source.getConfigurationId(),
            source.getMappingKey(), source.getMappingVersion(), source.getSourceContract(), source.getSourceVersion(),
            source.getTargetSchema(), source.getTargetVersion(), read(source.getRules()), source.getDefinitionHash(),
            source.getLifecycle(), source.getCreatedAt(), source.getCreatedBy());
    }

    private IntegrationExchangeEntity exchangeToEntity(IntegrationExchange source) {
        var target = new IntegrationExchangeEntity();
        target.setId(source.id());
        target.setTenantId(source.tenantId());
        target.setConfigurationId(source.configurationId());
        target.setSourceEventId(source.sourceEventId());
        target.setSourceEventType(source.sourceEventType());
        target.setMappingVersionId(source.mappingVersionId());
        target.setMappingDefinitionHash(source.mappingDefinitionHash());
        target.setCanonicalPayload(source.canonicalPayload());
        target.setPayloadHash(source.payloadHash());
        target.setStatus(source.status());
        target.setAttemptCount(source.attemptCount());
        target.setNextAttemptAt(source.nextAttemptAt());
        target.setLockedUntil(source.lockedUntil());
        target.setExternalCorrelationId(source.externalCorrelationId());
        target.setTargetFilename(source.targetFilename());
        target.setLastErrorCode(source.lastErrorCode());
        target.setCreatedAt(source.createdAt());
        target.setUpdatedAt(source.updatedAt());
        target.setCompletedAt(source.completedAt());
        target.setVersion(source.version());
        return target;
    }

    private IntegrationExchange exchangeToDomain(IntegrationExchangeEntity source) {
        return new IntegrationExchange(source.getId(), source.getTenantId(), source.getConfigurationId(),
            source.getSourceEventId(), source.getSourceEventType(), source.getMappingVersionId(),
            source.getMappingDefinitionHash(), source.getCanonicalPayload(), source.getPayloadHash(),
            source.getStatus(), source.getAttemptCount(), source.getNextAttemptAt(), source.getLockedUntil(),
            source.getExternalCorrelationId(), source.getTargetFilename(), source.getLastErrorCode(),
            source.getCreatedAt(), source.getUpdatedAt(), source.getCompletedAt(), source.getVersion());
    }

    private IntegrationExchangeAttemptEntity attemptToEntity(IntegrationExchangeAttempt source) {
        var target = new IntegrationExchangeAttemptEntity();
        target.setId(source.id());
        target.setTenantId(source.tenantId());
        target.setExchangeId(source.exchangeId());
        target.setAttemptNumber(source.attemptNumber());
        target.setStartedAt(source.startedAt());
        target.setCompletedAt(source.completedAt());
        target.setLatencyMillis(source.latencyMillis());
        target.setOutcome(source.outcome());
        target.setErrorCode(source.errorCode());
        target.setExternalCorrelationId(source.externalCorrelationId());
        target.setTargetFilename(source.targetFilename());
        return target;
    }

    private IntegrationExchangeAttempt attemptToDomain(IntegrationExchangeAttemptEntity source) {
        return new IntegrationExchangeAttempt(source.getId(), source.getTenantId(), source.getExchangeId(),
            source.getAttemptNumber(), source.getStartedAt(), source.getCompletedAt(), source.getLatencyMillis(),
            source.getOutcome(), source.getErrorCode(), source.getExternalCorrelationId(), source.getTargetFilename());
    }

    private IntegrationAuditEventEntity auditToEntity(IntegrationAuditEvent source) {
        var target = new IntegrationAuditEventEntity();
        target.setId(source.id());
        target.setTenantId(source.tenantId());
        target.setActor(source.actor());
        target.setAction(source.action());
        target.setTargetType(source.targetType());
        target.setTargetId(source.targetId());
        target.setOutcome(source.outcome());
        target.setSafeCode(source.safeCode());
        target.setBeforeHash(source.beforeHash());
        target.setAfterHash(source.afterHash());
        target.setCorrelationId(source.correlationId());
        target.setOccurredAt(source.occurredAt());
        return target;
    }

    private String write(List<IntegrationMapping.Rule> rules) {
        try {
            return json.writeValueAsString(rules);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Mapping rules cannot be serialized", exception);
        }
    }

    private List<IntegrationMapping.Rule> read(String rules) {
        try {
            return json.readValue(rules, RULES_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored mapping rules are invalid", exception);
        }
    }
}
