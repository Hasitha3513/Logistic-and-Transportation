package com.transportlogistics.app.shared.web;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiError(String code, String message, OffsetDateTime timestamp, List<FieldViolation> violations) {
    public record FieldViolation(String field, String message) {
    }
}
