package com.transportlogistics.app.tracking.adapters.inbound.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import java.math.BigDecimal;
import java.time.Instant;

abstract class AbstractJsonTelemetryPayloadNormalizer {
    private final ObjectMapper objectMapper;

    AbstractJsonTelemetryPayloadNormalizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    final JsonNode json(String rawPayload) {
        if (rawPayload == null || rawPayload.isBlank()) {
            throw invalid("Telemetry payload is required");
        }
        try {
            var root = objectMapper.readTree(rawPayload);
            if (root == null || !root.isObject()) {
                throw invalid("Telemetry payload must be a JSON object");
            }
            return root;
        } catch (JsonProcessingException exception) {
            throw invalid("Malformed telemetry JSON");
        }
    }

    static JsonNode at(JsonNode root, String pointer, boolean required) {
        var value = root.at(pointer);
        if (required && (value.isMissingNode() || value.isNull())) {
            throw invalid("Required telemetry field is missing");
        }
        return value;
    }

    static String text(JsonNode value, boolean required) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            if (required) {
                throw invalid("Required telemetry field is missing");
            }
            return null;
        }
        var result = value.asText();
        if (required && result.isBlank()) {
            throw invalid("Required telemetry field is blank");
        }
        return result;
    }

    static BigDecimal decimal(JsonNode value, boolean required) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            if (required) {
                throw invalid("Required telemetry field is missing");
            }
            return null;
        }
        try {
            return value.decimalValue();
        } catch (RuntimeException exception) {
            throw invalid("Telemetry number is invalid");
        }
    }

    static Instant instant(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            throw invalid("Telemetry timestamp is required");
        }
        try {
            return value.isNumber()
                    ? Instant.ofEpochSecond(value.longValue())
                    : Instant.parse(value.asText());
        } catch (RuntimeException exception) {
            throw invalid("Telemetry timestamp is invalid");
        }
    }

    static BusinessRuleException invalid(String message) {
        return new BusinessRuleException("TRACKING_POSITION_INVALID", message);
    }
}
