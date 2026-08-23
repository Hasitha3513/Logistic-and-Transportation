package com.transportlogistics.app.offlinesync.infrastructure.adapters.out.trip;

import com.fasterxml.jackson.databind.JsonNode;
import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncPayloadException;
import com.transportlogistics.app.trip.TripOperationalEventRecorder;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

final class TripOperationalEventPayloadParser {
    private static final Set<String> COMMON_FIELDS = Set.of(
            "occurredAt", "locationId", "locationDescription", "remarks");

    private TripOperationalEventPayloadParser() {
    }

    static CheckpointPayload checkpoint(JsonNode payload) {
        requireObjectWithFields(payload, union(COMMON_FIELDS, "checkpointType"));
        return new CheckpointPayload(enumValue(payload, "checkpointType", TripOperationalEventRecorder.CheckpointType.class),
                occurredAt(payload), optionalUuid(payload, "locationId"), optionalText(payload, "locationDescription", 255),
                optionalText(payload, "remarks", 2000));
    }

    static DelayPayload delay(JsonNode payload) {
        requireObjectWithFields(payload, union(COMMON_FIELDS, "delayMinutes", "reason"));
        JsonNode minutes = payload.get("delayMinutes");
        if (minutes == null || !minutes.isIntegralNumber() || !minutes.canConvertToInt() || minutes.intValue() < 1) {
            invalid("Delay minutes must be an integer of at least 1");
        }
        return new DelayPayload(minutes.intValue(), requiredText(payload, "reason", 500), occurredAt(payload),
                optionalUuid(payload, "locationId"), optionalText(payload, "locationDescription", 255),
                optionalText(payload, "remarks", 2000));
    }

    static IncidentPayload incident(JsonNode payload) {
        requireObjectWithFields(payload, union(COMMON_FIELDS, "incidentSeverity", "description"));
        return new IncidentPayload(enumValue(payload, "incidentSeverity",
                TripOperationalEventRecorder.IncidentSeverity.class), requiredText(payload, "description", 500),
                occurredAt(payload), optionalUuid(payload, "locationId"),
                optionalText(payload, "locationDescription", 255), optionalText(payload, "remarks", 2000));
    }

    private static OffsetDateTime occurredAt(JsonNode payload) {
        JsonNode value = payload.get("occurredAt");
        if (value == null || !value.isTextual()) invalid("Occurred time is required");
        try {
            return OffsetDateTime.parse(value.textValue());
        } catch (DateTimeParseException exception) {
            throw new OfflineSyncPayloadException("Occurred time must be a valid ISO offset date-time");
        }
    }

    private static UUID optionalUuid(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        if (value == null || value.isNull()) return null;
        if (!value.isTextual()) invalid(field + " must be a UUID");
        try {
            return UUID.fromString(value.textValue());
        } catch (IllegalArgumentException exception) {
            throw new OfflineSyncPayloadException(field + " must be a UUID");
        }
    }

    private static String requiredText(JsonNode payload, String field, int maximumLength) {
        String value = optionalText(payload, field, maximumLength);
        if (value == null || value.isBlank()) invalid(field + " is required");
        return value;
    }

    private static String optionalText(JsonNode payload, String field, int maximumLength) {
        JsonNode value = payload.get(field);
        if (value == null || value.isNull()) return null;
        if (!value.isTextual()) invalid(field + " must be text");
        String normalized = value.textValue().trim();
        if (normalized.length() > maximumLength) invalid(field + " cannot exceed " + maximumLength + " characters");
        return normalized.isBlank() ? null : normalized;
    }

    private static <E extends Enum<E>> E enumValue(JsonNode payload, String field, Class<E> type) {
        JsonNode value = payload.get(field);
        if (value == null || !value.isTextual()) invalid(field + " is required");
        try {
            return Enum.valueOf(type, value.textValue());
        } catch (IllegalArgumentException exception) {
            throw new OfflineSyncPayloadException(field + " is unsupported");
        }
    }

    private static void requireObjectWithFields(JsonNode payload, Set<String> allowedFields) {
        if (payload == null || !payload.isObject()) invalid("Trip operational-event payload is invalid");
        Set<String> fields = new HashSet<>();
        Iterator<String> names = payload.fieldNames();
        names.forEachRemaining(fields::add);
        if (!allowedFields.containsAll(fields)) invalid("Trip operational-event payload contains unsupported fields");
    }

    private static Set<String> union(Set<String> common, String... fields) {
        Set<String> combined = new HashSet<>(common);
        combined.addAll(Set.of(fields));
        return Set.copyOf(combined);
    }

    private static void invalid(String message) {
        throw new OfflineSyncPayloadException(message);
    }

    record CheckpointPayload(TripOperationalEventRecorder.CheckpointType checkpointType, OffsetDateTime occurredAt,
                             UUID locationId, String locationDescription, String remarks) {
    }

    record DelayPayload(int delayMinutes, String reason, OffsetDateTime occurredAt, UUID locationId,
                        String locationDescription, String remarks) {
    }

    record IncidentPayload(TripOperationalEventRecorder.IncidentSeverity incidentSeverity, String description,
                           OffsetDateTime occurredAt, UUID locationId, String locationDescription, String remarks) {
    }
}
