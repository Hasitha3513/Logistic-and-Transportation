package com.transportlogistics.app.tracking.adapters.inbound.traccar;

import com.fasterxml.jackson.databind.JsonNode;
import com.transportlogistics.app.tracking.application.provider.NormalizedPositionCandidate;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV2.BatteryChargingState;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV2.ExternalPowerState;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV2.TamperState;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
final class TraccarMessageMapper {
    private static final BigDecimal KNOTS_TO_KPH = new BigDecimal("1.852");

    NormalizedPositionCandidate map(JsonNode position, String externalDeviceReference) {
        try {
            JsonNode attributes = position.path("attributes");
            BigDecimal speed = decimal(position, "speed");
            String alarm = text(attributes, "alarm");
            return new NormalizedPositionCandidate(
                    externalDeviceReference,
                    Instant.parse(requiredText(position, "fixTime")),
                    requiredDecimal(position, "latitude"),
                    requiredDecimal(position, "longitude"),
                    decimal(position, "accuracy"),
                    speed == null ? null : speed.multiply(KNOTS_TO_KPH),
                    decimal(position, "course"),
                    decimal(position, "altitude"),
                    engine(attributes.path("ignition")),
                    kilometres(attributes, "totalDistance"),
                    hours(attributes, "hours"),
                    text(position, "id"),
                    longValue(position, "id"),
                    alarm == null ? null : "tampering".equalsIgnoreCase(alarm)
                            ? TamperState.DETECTED : TamperState.UNKNOWN,
                    decimal(attributes, "batteryLevel"),
                    decimal(attributes, "battery"),
                    power(attributes.path("power")),
                    charging(attributes.path("charge")));
        } catch (RuntimeException exception) {
            if (exception instanceof TraccarFailure failure) {
                throw failure;
            }
            throw new TraccarFailure(
                    TraccarFailure.Kind.MAPPING, "provider_position_invalid", exception);
        }
    }

    private static String requiredText(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private static BigDecimal requiredDecimal(JsonNode node, String field) {
        BigDecimal value = decimal(node, field);
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.decimalValue();
    }

    private static Long longValue(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.canConvertToLong() ? value.longValue() : null;
    }

    private static BigDecimal kilometres(JsonNode attributes, String field) {
        BigDecimal meters = decimal(attributes, field);
        return meters == null ? null : meters.movePointLeft(3);
    }

    private static BigDecimal hours(JsonNode attributes, String field) {
        BigDecimal milliseconds = decimal(attributes, field);
        return milliseconds == null ? null
                : milliseconds.divide(new BigDecimal("3600000"), 6, java.math.RoundingMode.HALF_UP);
    }

    private static EngineState engine(JsonNode value) {
        return value.isBoolean()
                ? value.booleanValue() ? EngineState.ON : EngineState.OFF
                : EngineState.UNKNOWN;
    }

    private static ExternalPowerState power(JsonNode value) {
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.isBoolean() ? value.booleanValue()
                ? ExternalPowerState.CONNECTED : ExternalPowerState.DISCONNECTED
                : ExternalPowerState.UNKNOWN;
    }

    private static BatteryChargingState charging(JsonNode value) {
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.isBoolean() ? value.booleanValue()
                ? BatteryChargingState.CHARGING : BatteryChargingState.NOT_CHARGING
                : BatteryChargingState.UNKNOWN;
    }
}
