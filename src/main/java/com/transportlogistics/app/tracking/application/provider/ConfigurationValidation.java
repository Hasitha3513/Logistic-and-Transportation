package com.transportlogistics.app.tracking.application.provider;

import java.util.List;
import java.util.Objects;

public record ConfigurationValidation(Status status, List<ValidationIssue> issues) {
    public enum Status { VALID, INVALID }

    public ConfigurationValidation {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(issues, "issues");
        if (issues.size() > 32 || issues.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Validation issues are invalid");
        }
        if (status == Status.VALID && !issues.isEmpty()) {
            throw new IllegalArgumentException("Valid configuration cannot contain issues");
        }
        if (status == Status.INVALID && issues.isEmpty()) {
            throw new IllegalArgumentException("Invalid configuration requires an issue");
        }
        issues = List.copyOf(issues);
    }

    public static ConfigurationValidation valid() {
        return new ConfigurationValidation(Status.VALID, List.of());
    }

    public static ConfigurationValidation invalid(ValidationIssue... issues) {
        return new ConfigurationValidation(Status.INVALID, List.of(issues));
    }

    public record ValidationIssue(String code, String message) {
        public ValidationIssue {
            if (!bounded(code, 80) || !bounded(message, 300)) {
                throw new IllegalArgumentException("Validation issue is invalid");
            }
        }

        private static boolean bounded(String value, int maximum) {
            return value != null && !value.isBlank() && value.length() <= maximum;
        }
    }
}
