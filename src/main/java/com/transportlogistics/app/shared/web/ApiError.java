package com.transportlogistics.app.shared.web;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiError(OffsetDateTime timestamp, int status, String error, String code, String message, String path,
                       String correlationId, List<FieldViolation> fieldErrors) {
    public record FieldViolation(String field, String message) {
    }
}
