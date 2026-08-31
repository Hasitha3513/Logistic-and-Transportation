package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.domain.model.*;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryAttemptRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class DeliveryAttemptPersistenceAdapter implements DeliveryAttemptRepository {
    private final DeliveryAttemptJpaRepository repository;
    private final DeliveryTenantContextPort tenantContext;

    public DeliveryAttemptPersistenceAdapter(DeliveryAttemptJpaRepository repository,
                                            DeliveryTenantContextPort tenantContext) {
        this.repository = repository;
        this.tenantContext = tenantContext;
    }

    @Override
    public DeliveryAttempt save(DeliveryAttempt attempt) {
        UUID tenantId = requiredTenantId();
        DeliveryAttemptEntity entity = toEntity(attempt, tenantId);
        DeliveryAttemptEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<DeliveryAttempt> findById(UUID id) {
        UUID tenantId = requiredTenantId();
        return repository.findByIdAndTenantId(id, tenantId).map(this::toDomain);
    }

    @Override
    public List<DeliveryAttempt> findByDeliveryId(UUID deliveryId) {
        UUID tenantId = requiredTenantId();
        return repository.findByDeliveryIdAndTenantIdOrderByAttemptNumberAsc(deliveryId, tenantId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public int countByDeliveryId(UUID deliveryId) {
        UUID tenantId = requiredTenantId();
        return repository.countByDeliveryIdAndTenantId(deliveryId, tenantId);
    }

    @Override
    public Optional<DeliveryAttempt> findLatestByDeliveryId(UUID deliveryId) {
        UUID tenantId = requiredTenantId();
        return repository.findFirstByDeliveryIdAndTenantIdOrderByAttemptNumberDesc(deliveryId, tenantId)
                .map(this::toDomain);
    }

    private DeliveryAttemptEntity toEntity(DeliveryAttempt domain, UUID tenantId) {
        DeliveryAttemptEntity entity = new DeliveryAttemptEntity();
        entity.setId(domain.id());
        entity.setTenantId(tenantId);
        entity.setDeliveryId(domain.deliveryId().value());
        entity.setAttemptNumber(domain.attemptNumber());
        entity.setAttemptTimestamp(domain.attemptTimestamp());
        entity.setFailureReason(domain.failureReason().name());
        entity.setNotes(domain.notes());
        entity.setDisposition(domain.disposition().name());
        entity.setRecordedBy(domain.recordedBy());
        entity.setRecordedAt(domain.recordedAt());
        return entity;
    }

    private DeliveryAttempt toDomain(DeliveryAttemptEntity entity) {
        return new DeliveryAttempt(
                entity.getId(),
                new DeliveryId(entity.getDeliveryId()),
                entity.getAttemptNumber(),
                entity.getAttemptTimestamp(),
                DeliveryFailureReason.valueOf(entity.getFailureReason()),
                entity.getNotes(),
                DeliveryFailureDisposition.valueOf(entity.getDisposition()),
                List.of(),
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
