package com.transportlogistics.app.freight.manifest.domain.model;

public record ManifestValidationFailure(String code, String field, String message) { }
