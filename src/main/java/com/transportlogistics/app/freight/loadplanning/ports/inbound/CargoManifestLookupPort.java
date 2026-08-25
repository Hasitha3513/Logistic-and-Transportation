package com.transportlogistics.app.freight.loadplanning.ports.inbound;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for looking up Cargo Manifest data required for Load Planning.
 */
public interface CargoManifestLookupPort {

    record ManifestItemPlanningView(
            UUID itemId,
            String description,
            BigDecimal quantity,
            String packingInformation,
            String commodityClassification,
            boolean hazardous,
            String hazardousClassification
    ) {}

    record ManifestPlanningView(
            UUID manifestId,
            String manifestNumber,
            boolean finalized,
            List<ManifestItemPlanningView> items
    ) {}

    Optional<ManifestPlanningView> findManifest(UUID cargoManifestId);
}
