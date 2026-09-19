package com.transportlogistics.app.compliance.domain;

/** Opaque policy scope reference; it has no jurisdictional or legal meaning by itself. */
public record JurisdictionScopeReference(String jurisdictionCode, String scopeCode) {

    public JurisdictionScopeReference {
        jurisdictionCode = requiredCode(jurisdictionCode, "jurisdictionCode");
        scopeCode = requiredCode(scopeCode, "scopeCode");
    }

    private static String requiredCode(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        String normalized = value.trim();
        if (normalized.length() > 80 || !normalized.matches("[A-Za-z0-9][A-Za-z0-9_.:-]*")) {
            throw new IllegalArgumentException(name + " must be an opaque code of at most 80 characters");
        }
        return normalized;
    }
}
