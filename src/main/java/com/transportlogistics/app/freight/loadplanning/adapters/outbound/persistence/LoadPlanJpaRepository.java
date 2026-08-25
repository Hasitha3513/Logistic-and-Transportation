package com.transportlogistics.app.freight.loadplanning.adapters.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LoadPlanJpaRepository extends JpaRepository<LoadPlanEntity, UUID> {

    Optional<LoadPlanEntity> findByCargoManifestId(UUID cargoManifestId);

    Optional<LoadPlanEntity> findByLoadPlanNumber(String loadPlanNumber);
}
