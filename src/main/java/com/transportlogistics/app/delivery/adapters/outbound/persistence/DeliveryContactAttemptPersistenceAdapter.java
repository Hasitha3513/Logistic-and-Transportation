package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.domain.model.DeliveryContactAttempt;
import com.transportlogistics.app.delivery.domain.model.DeliveryContactChannel;
import com.transportlogistics.app.delivery.domain.model.DeliveryContactOutcome;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryContactAttemptRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class DeliveryContactAttemptPersistenceAdapter implements DeliveryContactAttemptRepository {
    private final DeliveryContactAttemptJpaRepository repository;
    private final DeliveryTenantContextPort tenantContext;

    public DeliveryContactAttemptPersistenceAdapter(DeliveryContactAttemptJpaRepository repository,
                                                   DeliveryTenantContextPort tenantContext) {
        this.repository = repository;
        this.tenantContext = tenantContext;
    }

    @Override
    public DeliveryContactAttempt save(DeliveryContactAttempt contactAttempt) {
        UUID tenantId = requiredTenantId();
        DeliveryContactAttemptEntity entity = toEntity(contactAttempt, tenantId);
        DeliveryContactAttemptEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<DeliveryContactAttempt> saveAll(List<DeliveryContactAttempt> contactAttempts) {
        UUID tenantId = requiredTenantId();
        List<DeliveryContactAttemptEntity> entities = contactAttempts.stream()
                .map(c -> toEntity(c, tenantId))
                .toList();
        return repository.saveAll(entities).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<DeliveryContactAttempt> findByDeliveryAttemptId(UUID deliveryAttemptId) {
        UUID tenantId = requiredTenantId();
        return repository.findByDeliveryAttemptIdAndTenantIdOrderByContactTimestampAsc(deliveryAttemptId, tenantId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<DeliveryContactAttempt> findById(UUID id) {
        UUID tenantId = requiredTenantId();
        return repository.findByIdAndTenantId(id, tenantId).map(this::toDomain);
    }

    private DeliveryContactAttemptEntity toEntity(DeliveryContactAttempt domain, UUID tenantId) {
        DeliveryContactAttemptEntity entity = new DeliveryContactAttemptEntity();
        entity.setId(domain.id());
        entity.setTenantId(tenantId);
        entity.setDeliveryAttemptId(domain.deliveryAttemptId());
        entity.setChannel(domain.channel().name());
        entity.setContactTimestamp(domain.contactTimestamp());
        entity.setOutcome(domain.outcome().name());
        entity.setNotes(domain.notes());
        entity.setRecordedBy(domain.recordedBy());
        entity.setRecordedAt(domain.recordedAt());
        return entity;
    }

    private DeliveryContactAttempt toDomain(DeliveryContactAttemptEntity entity) {
        return new DeliveryContactAttempt(
                entity.getId(),
                entity.getDeliveryAttemptId(),
                DeliveryContactChannel.valueOf(entity.getChannel()),
                entity.getContactTimestamp(),
                DeliveryContactOutcome.valueOf(entity.getOutcome()),
                entity.getNotes(),
                entity.getRecordedBy(),
                entity.getRecordedAt()
        );
    }

    private UUID requiredTenantId() {
        return tenantContext.currentTenant()
                .map(DeliveryTenantContextPort.TenantContext::tenantId)
                .orElseThrow(() -> new BusinessRuleException("TENANT_CONTEXT_MISSING", "Tenant context is required"));
    }
}
