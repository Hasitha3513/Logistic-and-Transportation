package com.transportlogistics.app.tenancy.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.tenancy.application.ports.out.TenantRepository;
import com.transportlogistics.app.tenancy.domain.model.Tenant;
import org.springframework.stereotype.Component;

import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

@Component
class TenantPersistenceAdapter implements TenantRepository {
    private final TenantJpaRepository repository;

    TenantPersistenceAdapter(TenantJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Tenant> findById(UUID tenantId) {
        return repository.findById(tenantId).map(this::toDomain);
    }

    @Override
    public java.util.List<Tenant> findActive() {
        return repository.findByStatusOrderByTenantId(
                com.transportlogistics.app.tenancy.domain.model.TenantStatus.ACTIVE).stream().map(this::toDomain).toList();
    }

    private Tenant toDomain(TenantEntity entity) {
        return new Tenant(entity.getTenantId(), entity.getTenantCode(),
                entity.getTenantName(), Currency.getInstance(entity.getDefaultCurrency()), entity.getDefaultTimeZone(),
                entity.getStatus(), entity.getCreatedAt(), entity.getCreatedBy(), entity.getUpdatedAt(),
                entity.getUpdatedBy(), entity.getVersion());
    }
}
