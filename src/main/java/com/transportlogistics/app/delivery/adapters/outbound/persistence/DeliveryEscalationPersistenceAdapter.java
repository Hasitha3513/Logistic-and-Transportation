package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.domain.model.DeliveryEscalation;
import com.transportlogistics.app.delivery.domain.model.DeliveryEscalationStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryId;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryEscalationRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class DeliveryEscalationPersistenceAdapter implements DeliveryEscalationRepository {
    private final DeliveryEscalationJpaRepository repository;
    private final DeliveryTenantContextPort tenantContext;

    public DeliveryEscalationPersistenceAdapter(DeliveryEscalationJpaRepository repository,
                                               DeliveryTenantContextPort tenantContext) {
        this.repository = repository;
        this.tenantContext = tenantContext;
    }

    @Override
    public DeliveryEscalation save(DeliveryEscalation escalation) {
        UUID tenantId = requiredTenantId();
        DeliveryEscalationEntity entity = toEntity(escalation, tenantId);
        DeliveryEscalationEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<DeliveryEscalation> findById(UUID id) {
        UUID tenantId = requiredTenantId();
        return repository.findByIdAndTenantId(id, tenantId).map(this::toDomain);
    }

    @Override
    public List<DeliveryEscalation> findByDeliveryId(UUID deliveryId) {
        UUID tenantId = requiredTenantId();
        return repository.findByDeliveryIdAndTenantIdOrderByEscalatedAtAsc(deliveryId, tenantId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<DeliveryEscalation> findLatestByDeliveryId(UUID deliveryId) {
        UUID tenantId = requiredTenantId();
        return repository.findFirstByDeliveryIdAndTenantIdOrderByEscalatedAtDesc(deliveryId, tenantId)
                .map(this::toDomain);
    }

    @Override
    public List<DeliveryEscalation> findByStatus(DeliveryEscalationStatus status) {
        UUID tenantId = requiredTenantId();
        return repository.findByStatusAndTenantIdOrderByEscalatedAtDesc(status.name(), tenantId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private DeliveryEscalationEntity toEntity(DeliveryEscalation domain, UUID tenantId) {
        DeliveryEscalationEntity entity = new DeliveryEscalationEntity();
        entity.setId(domain.id());
        entity.setTenantId(tenantId);
        entity.setDeliveryId(domain.deliveryId().value());
        entity.setDeliveryAttemptId(domain.deliveryAttemptId());
        entity.setReason(domain.reason());
        entity.setStatus(domain.status().name());
        entity.setResolutionNotes(domain.resolutionNotes());
        entity.setEscalatedBy(domain.escalatedBy());
        entity.setEscalatedAt(domain.escalatedAt());
        entity.setResolvedBy(domain.resolvedBy());
        entity.setResolvedAt(domain.resolvedAt());
        return entity;
    }

    private DeliveryEscalation toDomain(DeliveryEscalationEntity entity) {
        return new DeliveryEscalation(
                entity.getId(),
                new DeliveryId(entity.getDeliveryId()),
                entity.getDeliveryAttemptId(),
                entity.getReason(),
                DeliveryEscalationStatus.valueOf(entity.getStatus()),
                entity.getResolutionNotes(),
                entity.getEscalatedBy(),
                entity.getEscalatedAt(),
                entity.getResolvedBy(),
                entity.getResolvedAt()
        );
    }

    private UUID requiredTenantId() {
        return tenantContext.currentTenant()
                .map(DeliveryTenantContextPort.TenantContext::tenantId)
                .orElseThrow(() -> new BusinessRuleException("TENANT_CONTEXT_MISSING", "Tenant context is required"));
    }
}
