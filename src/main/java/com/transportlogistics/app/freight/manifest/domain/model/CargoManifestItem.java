package com.transportlogistics.app.freight.manifest.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.math.BigDecimal;
import java.util.UUID;

public record CargoManifestItem(UUID id, UUID freightOrderLineId, String description, BigDecimal quantity,
                                String packingInformation, String commodityClassification,
                                boolean customsApplicable, String customsInformation,
                                boolean hazardous, String hazardousClassification, String hazardousDetails) {
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
    }

    public java.util.List<ManifestValidationFailure> validationFailures() {
        var failures = new java.util.ArrayList<ManifestValidationFailure>();
        if (customsApplicable && customsInformation == null)
            failures.add(new ManifestValidationFailure("CUSTOMS_INFORMATION_REQUIRED", "items." + id + ".customsInformation", "Customs information is required when customs is applicable"));
        if (hazardous && hazardousClassification == null)
            failures.add(new ManifestValidationFailure("HAZARDOUS_CLASSIFICATION_REQUIRED", "items." + id + ".hazardousClassification", "Hazardous classification is required for hazardous cargo"));
        if (hazardous && hazardousDetails == null)
            failures.add(new ManifestValidationFailure("HAZARDOUS_DETAILS_REQUIRED", "items." + id + ".hazardousDetails", "Hazardous details are required for hazardous cargo"));
        return java.util.List.copyOf(failures);
    }

    private static String required(String value, String field, int max) { String result = optional(value, max, field); if (result == null) throw invalid(field + " is required"); return result; }
    private static String requiredCode(String value, String field, int max) { String result = required(value, field, max).toUpperCase(java.util.Locale.ROOT); if (!result.matches("[A-Z0-9][A-Z0-9_.-]*")) throw invalid(field + " must be a provider-neutral code"); return result; }
    private static String optionalCode(String value, int max, String field) { String result = optional(value, max, field); if (result != null) { result = result.toUpperCase(java.util.Locale.ROOT); if (!result.matches("[A-Z0-9][A-Z0-9_.-]*")) throw invalid(field + " must be a provider-neutral code"); } return result; }
    private static String optional(String value, int max, String field) { String result = value == null || value.isBlank() ? null : value.trim(); if (result != null && result.length() > max) throw invalid(field + " must not exceed " + max + " characters"); return result; }
    private static BusinessRuleException invalid(String message) { return new BusinessRuleException("INVALID_CARGO_MANIFEST_ITEM", message); }
}
