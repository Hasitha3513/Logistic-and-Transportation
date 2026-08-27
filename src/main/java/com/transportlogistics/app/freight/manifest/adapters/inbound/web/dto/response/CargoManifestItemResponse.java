package com.transportlogistics.app.freight.manifest.adapters.inbound.web.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CargoManifestItemResponse(
        UUID id,
        UUID freightOrderLineId,
        String description,
        BigDecimal quantity,
        String packingInformation,
        String commodityClassification,
        boolean customsApplicable,
        String customsInformation,
        boolean hazardous,
        String hazardousClassification,
        String hazardousDetails,
        Boolean fragile,
        Boolean temperatureSensitive,
        BigDecimal unitWeight,
        String weightUnit,
        BigDecimal length,
        BigDecimal width,
        BigDecimal height,
        String dimensionUnit
) {
    public CargoManifestItemResponse(
            UUID id,
            UUID freightOrderLineId,
            String description,
            BigDecimal quantity,
            String packingInformation,
            String commodityClassification,
            boolean customsApplicable,
            String customsInformation,
            boolean hazardous,
            String hazardousClassification,
            String hazardousDetails,
            Boolean fragile,
            Boolean temperatureSensitive
    ) {
        this(id, freightOrderLineId, description, quantity, packingInformation, commodityClassification,
                customsApplicable, customsInformation, hazardous, hazardousClassification, hazardousDetails,
                fragile, temperatureSensitive, null, null, null, null, null, null);
    }
}
