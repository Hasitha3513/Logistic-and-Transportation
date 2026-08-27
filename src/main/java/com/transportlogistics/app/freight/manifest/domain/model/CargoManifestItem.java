package com.transportlogistics.app.freight.manifest.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CargoManifestItem(UUID id, UUID freightOrderLineId, String description, BigDecimal quantity,
                                String packingInformation, String commodityClassification,
                                boolean customsApplicable, String customsInformation,
                                boolean hazardous, String hazardousClassification, String hazardousDetails,
                                Boolean fragile, Boolean temperatureSensitive,
                                BigDecimal unitWeight, String weightUnit,
                                BigDecimal length, BigDecimal width, BigDecimal height,
                                String dimensionUnit) {

    public CargoManifestItem(UUID id, UUID freightOrderLineId, String description, BigDecimal quantity,
                             String packingInformation, String commodityClassification,
                             boolean customsApplicable, String customsInformation,
                             boolean hazardous, String hazardousClassification, String hazardousDetails) {
        this(id, freightOrderLineId, description, quantity, packingInformation, commodityClassification,
                customsApplicable, customsInformation, hazardous, hazardousClassification, hazardousDetails,
                null, null, null, null, null, null, null, null);
    }

    public CargoManifestItem(UUID id, UUID freightOrderLineId, String description, BigDecimal quantity,
                             String packingInformation, String commodityClassification,
                             boolean customsApplicable, String customsInformation,
                             boolean hazardous, String hazardousClassification, String hazardousDetails,
                             Boolean fragile, Boolean temperatureSensitive) {
        this(id, freightOrderLineId, description, quantity, packingInformation, commodityClassification,
                customsApplicable, customsInformation, hazardous, hazardousClassification, hazardousDetails,
                fragile, temperatureSensitive, null, null, null, null, null, null);
    }

    public CargoManifestItem {
        if (id == null) throw invalid("Manifest item id is required");
        if (freightOrderLineId == null) throw invalid("Freight order line reference is required");
        description = required(description, "Item description", 500);
        if (quantity == null || quantity.signum() <= 0) throw invalid("Manifest item quantity must be greater than zero");
        packingInformation = required(packingInformation, "Packing information", 500);
        commodityClassification = requiredCode(commodityClassification, "Commodity classification", 120);
        customsInformation = optional(customsInformation, 1000, "Customs information");
        hazardousClassification = optionalCode(hazardousClassification, 120, "Hazardous classification");
        hazardousDetails = optional(hazardousDetails, 1000, "Hazardous details");

        if (unitWeight != null && unitWeight.signum() <= 0) {
            throw invalid("Manifest item unit weight must be greater than zero");
        }
        if (length != null && length.signum() <= 0) {
            throw invalid("Manifest item length must be greater than zero");
        }
        if (width != null && width.signum() <= 0) {
            throw invalid("Manifest item width must be greater than zero");
        }
        if (height != null && height.signum() <= 0) {
            throw invalid("Manifest item height must be greater than zero");
        }

        weightUnit = optionalCode(weightUnit, 16, "Weight unit");
        if (weightUnit != null && !List.of("KG", "G", "TONNE").contains(weightUnit)) {
            throw invalid("Weight unit must be one of KG, G, TONNE");
        }
        if (unitWeight != null && weightUnit == null) {
            weightUnit = "KG";
        }

        dimensionUnit = optionalCode(dimensionUnit, 16, "Dimension unit");
        if (dimensionUnit != null && !List.of("M", "CM", "MM").contains(dimensionUnit)) {
            throw invalid("Dimension unit must be one of M, CM, MM");
        }
        if ((length != null || width != null || height != null) && dimensionUnit == null) {
            dimensionUnit = "M";
        }
    }

    public List<ManifestValidationFailure> validationFailures() {
        var failures = new java.util.ArrayList<ManifestValidationFailure>();
        if (customsApplicable && customsInformation == null)
            failures.add(new ManifestValidationFailure("CUSTOMS_INFORMATION_REQUIRED", "items." + id + ".customsInformation", "Customs information is required when customs is applicable"));
        if (hazardous && hazardousClassification == null)
            failures.add(new ManifestValidationFailure("HAZARDOUS_CLASSIFICATION_REQUIRED", "items." + id + ".hazardousClassification", "Hazardous classification is required for hazardous cargo"));
        if (hazardous && hazardousDetails == null)
            failures.add(new ManifestValidationFailure("HAZARDOUS_DETAILS_REQUIRED", "items." + id + ".hazardousDetails", "Hazardous details are required for hazardous cargo"));
        if (fragile == null || temperatureSensitive == null)
            failures.add(new ManifestValidationFailure(
                    "SPECIAL_CARGO_CLASSIFICATION_MISSING",
                    "items." + id + ".specialCargoClassification",
                    "Fragile and temperature-sensitive classifications must both be explicitly provided"
            ));
        return List.copyOf(failures);
    }

    private static String required(String value, String field, int max) { String result = optional(value, max, field); if (result == null) throw invalid(field + " is required"); return result; }
    private static String requiredCode(String value, String field, int max) { String result = required(value, field, max).toUpperCase(java.util.Locale.ROOT); if (!result.matches("[A-Z0-9][A-Z0-9_.-]*")) throw invalid(field + " must be a provider-neutral code"); return result; }
    private static String optionalCode(String value, int max, String field) { String result = optional(value, max, field); if (result != null) { result = result.toUpperCase(java.util.Locale.ROOT); if (!result.matches("[A-Z0-9][A-Z0-9_.-]*")) throw invalid(field + " must be a provider-neutral code"); } return result; }
    private static String optional(String value, int max, String field) { String result = value == null || value.isBlank() ? null : value.trim(); if (result != null && result.length() > max) throw invalid(field + " must not exceed " + max + " characters"); return result; }
    private static BusinessRuleException invalid(String message) { return new BusinessRuleException("INVALID_CARGO_MANIFEST_ITEM", message); }
}
