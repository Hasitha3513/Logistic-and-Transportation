package com.transportlogistics.app.tracking.adapters.inbound.flespi;

import com.fasterxml.jackson.databind.JsonNode;
import com.transportlogistics.app.tracking.application.provider.NormalizedPositionCandidate;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
final class FlespiMessageMapper {
    NormalizedPositionCandidate map(JsonNode source, String externalDeviceReference) {
        String ident = text(source, "ident");
        if (ident == null || !ident.equals(externalDeviceReference)) throw mapping("device_identity");
        Instant sourceTimestamp = timestamp(source.get("timestamp"));
        BigDecimal latitude = decimal(source, "position.latitude");
        BigDecimal longitude = decimal(source, "position.longitude");
        if (sourceTimestamp == null || latitude == null || longitude == null) throw mapping("mandatory_field");
        if (latitude.compareTo(new BigDecimal("-90")) < 0 || latitude.compareTo(new BigDecimal("90")) > 0
                || longitude.compareTo(new BigDecimal("-180")) < 0
                || longitude.compareTo(new BigDecimal("180")) > 0) {
            throw mapping("coordinates");
        }

        BigDecimal accuracy = optionalNonNegative(source, "position.accuracy");
        BigDecimal speed = optionalRange(source, "position.speed", BigDecimal.ZERO, new BigDecimal("400"));
        BigDecimal heading = optionalRange(source, "position.direction", BigDecimal.ZERO, new BigDecimal("360"));
        if (heading != null && heading.compareTo(new BigDecimal("360")) == 0) heading = null;

        return new NormalizedPositionCandidate(
                externalDeviceReference, sourceTimestamp, latitude, longitude,
                accuracy, speed, heading, null, EngineState.UNKNOWN,
                null, null, null, null);
    }

    private static Instant timestamp(JsonNode node) {
        if (node == null || !node.isNumber()) return null;
        try {
            BigDecimal value = node.decimalValue();
            long seconds = value.longValue();
            int nanos = value.subtract(BigDecimal.valueOf(seconds)).movePointRight(9).intValue();
            return Instant.ofEpochSecond(seconds, nanos);
        } catch (ArithmeticException | java.time.DateTimeException exception) {
            return null;
        }
    }

    private static String text(JsonNode source, String name) {
        JsonNode value = source.get(name);
        return value != null && value.isTextual() && !value.textValue().isBlank() ? value.textValue() : null;
    }

    private static BigDecimal decimal(JsonNode source, String name) {
        JsonNode value = source.get(name);
        return value != null && value.isNumber() ? value.decimalValue() : null;
    }

    private static BigDecimal optionalNonNegative(JsonNode source, String name) {
        BigDecimal value = decimal(source, name);
        return value == null || value.signum() < 0 ? null : value;
    }

    private static BigDecimal optionalRange(JsonNode source, String name, BigDecimal minimum, BigDecimal maximum) {
        BigDecimal value = decimal(source, name);
        return value == null || value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0 ? null : value;
    }

    private static FlespiFailure mapping(String category) {
        return new FlespiFailure(FlespiFailure.Kind.MAPPING, category, null);
    }
}
