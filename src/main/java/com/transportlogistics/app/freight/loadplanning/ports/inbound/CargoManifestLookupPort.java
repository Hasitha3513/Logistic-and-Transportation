package com.transportlogistics.app.freight.loadplanning.ports.inbound;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for looking up Cargo Manifest data required for Load Planning and Weight/Volume validation.
 */
public interface CargoManifestLookupPort {

    record ManifestItemPlanningView(
            UUID itemId,
            String description,
            BigDecimal quantity,
            String packingInformation,
            String commodityClassification,
            boolean hazardous,
            String hazardousClassification,
            Boolean fragile,
            Boolean temperatureSensitive,
            BigDecimal unitWeight,
            String weightUnit,
            BigDecimal length,
            BigDecimal width,
            BigDecimal height,
            String dimensionUnit
    ) {
        public ManifestItemPlanningView(
                UUID itemId,
                String description,
                BigDecimal quantity,
                String packingInformation,
                String commodityClassification,
                boolean hazardous,
                String hazardousClassification,
                Boolean fragile,
                Boolean temperatureSensitive
        ) {
            this(itemId, description, quantity, packingInformation, commodityClassification,
                    hazardous, hazardousClassification, fragile, temperatureSensitive,
                    null, null, null, null, null, null);
        }

        public ManifestItemPlanningView(
                UUID itemId,
                String description,
                BigDecimal quantity,
                String packingInformation,
                String commodityClassification,
                boolean hazardous,
                String hazardousClassification
        ) {
            this(itemId, description, quantity, packingInformation, commodityClassification,
                    hazardous, hazardousClassification, null, null,
                    null, null, null, null, null, null);
        }
    }

    record ManifestPlanningView(
            UUID manifestId,
            String manifestNumber,
            boolean finalized,
            List<ManifestItemPlanningView> items
    ) {}

    Optional<ManifestPlanningView> findManifest(UUID cargoManifestId);
}
