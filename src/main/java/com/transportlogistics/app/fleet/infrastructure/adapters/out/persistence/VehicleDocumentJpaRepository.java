package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.domain.model.VehicleDocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import java.time.LocalDate;

interface VehicleDocumentJpaRepository extends JpaRepository<VehicleDocumentEntity, UUID> {
    List<VehicleDocumentEntity> findByVehicleIdAndStatusNotOrderByCreatedAtDesc(UUID vehicleId,
                                                                                VehicleDocumentStatus status);

    List<VehicleDocumentEntity> findByVehicleIdAndActiveTrue(UUID vehicleId);

    List<VehicleDocumentEntity> findByActiveTrueAndMandatoryForDispatchTrueAndExpiryDateLessThanEqualOrderByExpiryDateAsc(
            LocalDate cutoffInclusive);

    boolean existsByVehicleIdAndDocumentTypeIgnoreCaseAndDocumentNumberIgnoreCaseAndActiveTrueAndIdNot(
            UUID vehicleId, String documentType, String documentNumber, UUID id);
}
