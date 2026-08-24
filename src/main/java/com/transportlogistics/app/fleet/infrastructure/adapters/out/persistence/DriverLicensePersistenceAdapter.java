package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.application.ports.out.DriverLicenseRepository;
import com.transportlogistics.app.fleet.domain.model.DriverLicense;
import com.transportlogistics.app.fleet.domain.model.DriverLicenseStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;

@Component
class DriverLicensePersistenceAdapter implements DriverLicenseRepository {
    private final DriverLicenseJpaRepository repository;

    DriverLicensePersistenceAdapter(DriverLicenseJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public DriverLicense save(DriverLicense license) {
        var entity = new DriverLicenseEntity();
        entity.setId(license.id());
        entity.setDriverId(license.driverId());
        entity.setLicenseNumber(license.licenseNumber());
        entity.setLicenseClass(license.licenseClass());
        entity.setIssueDate(license.issueDate());
        entity.setExpiryDate(license.expiryDate());
        entity.setStatus(license.status());
        entity.setActive(license.active());
        entity.setCreatedAt(license.createdAt());
        entity.setUpdatedAt(license.updatedAt());
        entity.setCreatedBy(license.createdBy());
        entity.setUpdatedBy(license.updatedBy());
        return map(repository.save(entity));
    }

    @Override
    public Optional<DriverLicense> findById(UUID id) {
        return repository.findById(id).map(this::map);
    }

    @Override
    public List<DriverLicense> findVisibleByDriverId(UUID driverId) {
        return repository.findByDriverIdAndStatusNotOrderByCreatedAtDesc(driverId, DriverLicenseStatus.DELETED)
                .stream().map(this::map).toList();
    }

    @Override
    public List<DriverLicense> findActiveByDriverId(UUID driverId) {
        return repository.findByDriverIdAndActiveTrue(driverId).stream().map(this::map).toList();
    }

    @Override
    public List<DriverLicense> findActiveExpiringBy(LocalDate cutoffInclusive) {
        return repository.findByActiveTrueAndExpiryDateLessThanEqualOrderByExpiryDateAsc(cutoffInclusive)
                .stream().map(this::map).toList();
    }

    @Override
    public boolean licenseNumberExists(String licenseNumber, UUID excludedId) {
        return repository.existsByLicenseNumberIgnoreCaseAndIdNot(licenseNumber,
                excludedId == null ? new UUID(0, 0) : excludedId);
    }

    private DriverLicense map(DriverLicenseEntity entity) {
        return new DriverLicense(entity.getId(), entity.getDriverId(), entity.getLicenseNumber(),
                entity.getLicenseClass(), entity.getIssueDate(), entity.getExpiryDate(), entity.getStatus(),
                entity.isActive(), entity.getCreatedAt(), entity.getUpdatedAt(), entity.getCreatedBy(),
                entity.getUpdatedBy());
    }
}
