package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.domain.model.DeliveryRedeliverySchedule;
import com.transportlogistics.app.delivery.domain.model.RedeliveryScheduleStatus;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryRedeliveryScheduleRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class DeliveryRedeliverySchedulePersistenceAdapter implements DeliveryRedeliveryScheduleRepository {

    private final DeliveryRedeliveryScheduleJpaRepository repository;
    private final DeliveryTenantContextPort tenantContext;

    public DeliveryRedeliverySchedulePersistenceAdapter(
            DeliveryRedeliveryScheduleJpaRepository repository,
            DeliveryTenantContextPort tenantContext
    ) {
        this.repository = repository;
        this.tenantContext = tenantContext;
    }

    @Override
    public DeliveryRedeliverySchedule save(DeliveryRedeliverySchedule schedule) {
        DeliveryRedeliveryScheduleEntity entity = DeliveryRedeliveryScheduleEntity.fromDomain(schedule);
        return repository.save(entity).toDomain();
    }

    @Override
    public Optional<DeliveryRedeliverySchedule> findById(UUID id) {
        return tenantContext.currentTenant()
                .flatMap(tc -> repository.findByIdAndTenantId(id, tc.tenantId()))
                .map(DeliveryRedeliveryScheduleEntity::toDomain);
    }

    @Override
    public Optional<DeliveryRedeliverySchedule> findCurrentConfirmed(UUID deliveryOrderId) {
        return tenantContext.currentTenant()
                .flatMap(tc -> repository.findFirstByTenantIdAndDeliveryOrderIdAndStatusOrderByCreatedAtDesc(
                        tc.tenantId(), deliveryOrderId, RedeliveryScheduleStatus.CONFIRMED))
                .map(DeliveryRedeliveryScheduleEntity::toDomain);
    }

    @Override
    public List<DeliveryRedeliverySchedule> findByDeliveryOrderId(UUID deliveryOrderId) {
        return tenantContext.currentTenant()
                .map(tc -> repository.findByTenantIdAndDeliveryOrderIdOrderByCreatedAtDesc(tc.tenantId(), deliveryOrderId)
                        .stream()
                        .map(DeliveryRedeliveryScheduleEntity::toDomain)
                        .toList())
                .orElse(List.of());
    }

    @Override
    public int countActiveOverlapping(UUID tenantId, OffsetDateTime start, OffsetDateTime end, UUID excludeScheduleId) {
        return repository.countActiveOverlapping(tenantId, start, end, excludeScheduleId);
    }
}
