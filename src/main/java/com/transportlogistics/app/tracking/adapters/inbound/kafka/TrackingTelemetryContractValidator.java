package com.transportlogistics.app.tracking.adapters.inbound.kafka;

import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV1;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

final class TrackingTelemetryContractValidator {
    private static final Pattern DEDUPE = Pattern.compile("[0-9a-f]{64}");

    private TrackingTelemetryContractValidator() {
    }

    static void validate(
            ConsumerRecord<String, TrackingTelemetryIngestedV1> record,
            TrackingTelemetryIngestedV1 event,
            String expectedTopic) {
        if (!expectedTopic.equals(record.topic()) || event == null
                || !TrackingTelemetryIngestedV1.TYPE.equals(event.eventType())
                || event.eventVersion() != TrackingTelemetryIngestedV1.VERSION) {
            throw invalid("Unsupported telemetry event contract");
        }
        String expectedKey = event.tenantId() + ":" + event.vehicleId();
        if (!expectedKey.equals(record.key())
                || !event.tenantId().equals(uuidHeader(record, "tenantId"))
                || !event.eventType().equals(textHeader(record, "eventType"))
                || !Integer.toString(event.eventVersion()).equals(textHeader(record, "eventVersion"))) {
            throw invalid("Telemetry authority mismatch");
        }
        if (event.eventId() == null || event.tenantId() == null || event.vehicleId() == null
                || event.deviceId() == null || blank(event.providerAlias())
                || event.providerAlias().length() > 80 || event.providerMessageId() != null
                && event.providerMessageId().length() > 160
                || event.dedupeIdentity() == null || !DEDUPE.matcher(event.dedupeIdentity()).matches()
                || event.latitude() == null || event.latitude().compareTo(java.math.BigDecimal.valueOf(-90)) < 0
                || event.latitude().compareTo(java.math.BigDecimal.valueOf(90)) > 0
                || event.longitude() == null || event.longitude().compareTo(java.math.BigDecimal.valueOf(-180)) < 0
                || event.longitude().compareTo(java.math.BigDecimal.valueOf(180)) > 0
                || event.speedKph() != null && (event.speedKph().signum() < 0
                || event.speedKph().compareTo(java.math.BigDecimal.valueOf(400)) > 0)
                || event.headingDegrees() != null && (event.headingDegrees().signum() < 0
                || event.headingDegrees().compareTo(java.math.BigDecimal.valueOf(360)) >= 0)
                || event.horizontalAccuracyMeters() != null
                && event.horizontalAccuracyMeters().signum() < 0
                || event.recordedAt() == null || event.receivedAt() == null
                || event.recordedAt().equals(Instant.MIN) || event.receivedAt().equals(Instant.MIN)) {
            throw invalid("Invalid normalized telemetry fact");
        }
    }

    private static UUID uuidHeader(ConsumerRecord<?, ?> record, String name) {
        try {
            return UUID.fromString(textHeader(record, name));
        } catch (RuntimeException exception) {
            throw invalid("Invalid telemetry authority header");
        }
    }

    private static String textHeader(ConsumerRecord<?, ?> record, String name) {
        Header header = record.headers().lastHeader(name);
        if (header == null || header.value() == null) {
            throw invalid("Missing telemetry authority header");
        }
        return new String(header.value(), StandardCharsets.UTF_8);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static TelemetryContractException invalid(String message) {
        return new TelemetryContractException(message);
    }

    static final class TelemetryContractException extends IllegalArgumentException {
        TelemetryContractException(String message) {
            super(message);
        }
    }
}
