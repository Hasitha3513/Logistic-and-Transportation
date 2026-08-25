package com.transportlogistics.app.freight.loadplanning.ports.outbound;

import com.transportlogistics.app.freight.loadplanning.domain.LoadPlan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for persisting and retrieving LoadPlan aggregates.
 */
public interface LoadPlanRepository {

    LoadPlan save(LoadPlan loadPlan);

    Optional<LoadPlan> findById(UUID loadPlanId);

    Optional<LoadPlan> findByCargoManifestId(UUID cargoManifestId);

    List<LoadPlan> findAll();
}
