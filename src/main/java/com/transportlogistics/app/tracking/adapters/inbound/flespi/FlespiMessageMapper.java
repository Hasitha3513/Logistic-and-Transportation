package com.transportlogistics.app.tracking.adapters.inbound.flespi;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
final class FlespiMessageMapper {
    private final FlespiAdapterProperties properties;

    FlespiMessageMapper(FlespiAdapterProperties properties) { this.properties = properties; }

    MappedPosition map(JsonNode source) {
        String ident = text(source, "ident");
        if (ident == null || !ident.equals(properties.getDeviceIdent())) throw mapping("device_identity");
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

        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("source", "flespi-rest");
        return new MappedPosition(properties.getTrackingDeviceId(), sourceTimestamp, latitude, longitude,
                accuracy, speed, heading, Map.copyOf(metadata));
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

    record MappedPosition(java.util.UUID deviceId, Instant sourceTimestamp, BigDecimal latitude,
                          BigDecimal longitude, BigDecimal horizontalAccuracyMeters, BigDecimal speedKph,
                          BigDecimal headingDegrees, Map<String, String> safeMetadata) {}
}
