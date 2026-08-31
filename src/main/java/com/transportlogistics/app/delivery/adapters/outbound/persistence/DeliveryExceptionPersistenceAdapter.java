package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.domain.model.*;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryExceptionRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class DeliveryExceptionPersistenceAdapter implements DeliveryExceptionRepository {
    private final DeliveryExceptionJpaRepository repository;
    private final DeliveryTenantContextPort tenantContext;

    public DeliveryExceptionPersistenceAdapter(DeliveryExceptionJpaRepository repository, DeliveryTenantContextPort tenantContext) {
        this.repository = repository;
        this.tenantContext = tenantContext;
    }

    @Override
    public DeliveryExceptionCase save(DeliveryExceptionCase exceptionCase) {
        UUID tenantId = requiredTenant();
        DeliveryExceptionCaseEntity entity = toEntity(exceptionCase, tenantId);
        DeliveryExceptionCaseEntity saved = repository.saveAndFlush(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<DeliveryExceptionCase> findById(UUID id) {
        UUID tenantId = requiredTenant();
        return repository.findByIdAndTenantId(id, tenantId).map(this::toDomain);
    }

    @Override
    public List<DeliveryExceptionCase> findByDeliveryOrderId(DeliveryId deliveryOrderId) {
        UUID tenantId = requiredTenant();
        return repository.findByTenantIdAndDeliveryOrderIdOrderByReportedAtDesc(tenantId, deliveryOrderId.value())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsActiveByDeliveryOrderIdAndType(DeliveryId deliveryOrderId, DeliveryExceptionType exceptionType) {
        UUID tenantId = requiredTenant();
        return repository.existsByTenantIdAndDeliveryOrderIdAndExceptionTypeAndStatusIn(
                tenantId, deliveryOrderId.value(), exceptionType,
                Set.of(DeliveryExceptionStatus.OPEN, DeliveryExceptionStatus.UNDER_INVESTIGATION)
        );
    }

    @Override
    public boolean hasActiveBlockingExceptions(DeliveryId deliveryOrderId) {
        UUID tenantId = requiredTenant();
        return repository.hasActiveBlockingExceptions(tenantId, deliveryOrderId.value());
    }

    private UUID requiredTenant() {
        return tenantContext.currentTenant()
                .map(DeliveryTenantContextPort.TenantContext::tenantId)
                .orElseThrow(() -> new BusinessRuleException("TENANT_REQUIRED", "Active tenant context is required"));
    }

    private DeliveryExceptionCaseEntity toEntity(DeliveryExceptionCase domain, UUID tenantId) {
        DeliveryExceptionCaseEntity entity = new DeliveryExceptionCaseEntity();
        entity.setId(domain.id());
        entity.setTenantId(tenantId);
        entity.setDeliveryOrderId(domain.deliveryOrderId().value());
        entity.setDeliveryAttemptId(domain.deliveryAttemptId());
        entity.setExceptionType(domain.exceptionType());
        entity.setSeverity(domain.severity());
        entity.setStatus(domain.status());
        entity.setDescription(domain.description());
        entity.setCorrectedLocationId(domain.correctedLocationId());
        entity.setOtpAttemptReference(domain.otpAttemptReference());
        entity.setDeliveredItemsDescription(domain.deliveredItemsDescription());
        entity.setUndeliveredItemsDescription(domain.undeliveredItemsDescription());
        entity.setQuantityDelivered(domain.quantityDelivered());
        entity.setQuantityUndelivered(domain.quantityUndelivered());

        if (domain.resolution() != null) {
            entity.setResolutionCode(domain.resolution().resolutionCode());
            entity.setResolutionNotes(domain.resolution().resolutionNotes());
            entity.setFollowUpDisposition(domain.resolution().followUpDisposition());
            entity.setResolvedAt(domain.resolution().resolvedAt());
            entity.setResolvedBy(domain.resolution().resolvedBy());
        } else {
            entity.setResolutionCode(null);
            entity.setResolutionNotes(null);
            entity.setFollowUpDisposition(null);
            entity.setResolvedAt(null);
            entity.setResolvedBy(null);
        }

        entity.setVersion(domain.version());
        entity.setReportedAt(domain.reportedAt());
        entity.setReportedBy(domain.reportedBy());
        entity.setCreatedAt(domain.reportedAt());
        entity.setUpdatedAt(domain.resolvedAt() != null ? domain.resolvedAt() : domain.reportedAt());

        List<DeliveryExceptionEvidenceEntity> evidenceEntities = new ArrayList<>();
        if (domain.evidence() != null) {
            for (var ev : domain.evidence()) {
                DeliveryExceptionEvidenceEntity evEntity = new DeliveryExceptionEvidenceEntity();
                evEntity.setId(ev.id());
                evEntity.setTenantId(tenantId);
                evEntity.setExceptionCase(entity);
                evEntity.setStorageReference(ev.storageReference());
                evEntity.setDetectedContentType(ev.detectedContentType());
                evEntity.setContentLength(ev.contentLength());
                evEntity.setSha256Checksum(ev.sha256Checksum());
                evEntity.setOriginalFilename(ev.originalFilename());
                evEntity.setCreatedBy(ev.createdBy());
                evEntity.setCreatedAt(ev.createdAt());
                evidenceEntities.add(evEntity);
            }
        }
        entity.setEvidence(evidenceEntities);

        return entity;
    }

    private DeliveryExceptionCase toDomain(DeliveryExceptionCaseEntity entity) {
        DeliveryExceptionResolution resolution = null;
        if (entity.getResolutionCode() != null && entity.getResolvedAt() != null && entity.getResolvedBy() != null) {
            resolution = new DeliveryExceptionResolution(
                    entity.getResolutionCode(),
                    entity.getResolutionNotes(),
                    entity.getFollowUpDisposition(),
                    entity.getResolvedAt(),
                    entity.getResolvedBy()
            );
        }

        List<DeliveryExceptionEvidence> evidenceList = new ArrayList<>();
        if (entity.getEvidence() != null) {
            for (var ev : entity.getEvidence()) {
                evidenceList.add(new DeliveryExceptionEvidence(
                        ev.getId(),
                        entity.getId(),
                        ev.getStorageReference(),
                        ev.getDetectedContentType(),
                        ev.getContentLength(),
                        ev.getSha256Checksum(),
                        ev.getOriginalFilename(),
                        ev.getCreatedBy(),
                        ev.getCreatedAt()
                ));
            }
        }

        return new DeliveryExceptionCase(
                entity.getId(),
                new DeliveryId(entity.getDeliveryOrderId()),
                entity.getDeliveryAttemptId(),
                entity.getExceptionType(),
                entity.getSeverity(),
                entity.getStatus(),
                entity.getDescription(),
                entity.getCorrectedLocationId(),
                entity.getOtpAttemptReference(),
                entity.getDeliveredItemsDescription(),
                entity.getUndeliveredItemsDescription(),
                entity.getQuantityDelivered(),
                entity.getQuantityUndelivered(),
                resolution,
                entity.getVersion(),
                entity.getReportedAt(),
                entity.getReportedBy(),
                entity.getResolvedAt(),
                entity.getResolvedBy(),
                evidenceList
        );
    }
}
