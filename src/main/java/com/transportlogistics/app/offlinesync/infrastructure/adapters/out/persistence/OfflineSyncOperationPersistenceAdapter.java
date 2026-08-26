package com.transportlogistics.app.offlinesync.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.offlinesync.application.ports.out.OfflineSyncOperationRepository;
import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncDuplicateClaimException;
import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncOperation;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class OfflineSyncOperationPersistenceAdapter implements OfflineSyncOperationRepository {
    private final OfflineSyncOperationJpaRepository repository;

    public OfflineSyncOperationPersistenceAdapter(OfflineSyncOperationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<OfflineSyncOperation> findByOperationId(UUID operationId) {
        return repository.findById(operationId).map(OfflineSyncOperationEntity::toDomain);
    }

    @Override
    public OfflineSyncOperation claim(OfflineSyncOperation operation) {
        try {
            return repository.saveAndFlush(OfflineSyncOperationEntity.fromDomain(operation)).toDomain();
        } catch (DataIntegrityViolationException exception) {
            throw new OfflineSyncDuplicateClaimException(exception);
        }
    }

    @Override
    public OfflineSyncOperation save(OfflineSyncOperation operation) {
        return repository.save(OfflineSyncOperationEntity.fromDomain(operation)).toDomain();
    }
}
