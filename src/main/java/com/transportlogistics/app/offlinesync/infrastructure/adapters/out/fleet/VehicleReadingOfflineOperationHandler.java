package com.transportlogistics.app.offlinesync.infrastructure.adapters.out.fleet;

import com.fasterxml.jackson.databind.JsonNode;
import com.transportlogistics.app.fleet.ManualVehicleReadingRecorder;
import com.transportlogistics.app.offlinesync.application.ports.out.OfflineOperationHandler;
import com.transportlogistics.app.offlinesync.domain.model.OfflineHandlerOutcome;
import com.transportlogistics.app.offlinesync.domain.model.OfflineOperationContext;
import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncConflictException;
import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncPayloadException;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Component
final class VehicleReadingOfflineOperationHandler implements OfflineOperationHandler {
    private static final Set<String> ALLOWED_FIELDS = Set.of("readingType", "value", "recordedAt", "notes");
    private final ManualVehicleReadingRecorder readings;

    VehicleReadingOfflineOperationHandler(ManualVehicleReadingRecorder readings) {
        this.readings = readings;
    }

    @Override
    public String operationType() {
        return "VEHICLE_READING_RECORD";
    }

    @Override
    public int operationVersion() {
        return 1;
    }

    @Override
    public Set<String> requiredAuthorities() {
        return Set.of("VEHICLE_READING_CREATE");
    }

    @Override
    public OfflineHandlerOutcome apply(OfflineOperationContext context, JsonNode payload) {
        ParsedPayload parsed = parse(payload);
        try {
            readings.recordManual(new ManualVehicleReadingRecorder.Command(
                    context.aggregateId(), parsed.readingType(), parsed.value(), parsed.recordedAt(),
                    context.actorId(), context.operationId().toString(), parsed.notes()));
            return OfflineHandlerOutcome.applied();
        } catch (NotFoundException exception) {
            return OfflineHandlerOutcome.rejected(exception.code(), exception.getMessage());
        } catch (ConflictException exception) {
            throw new OfflineSyncConflictException(exception.getMessage());
        } catch (BusinessRuleException exception) {
            throw new OfflineSyncConflictException(exception.getMessage());
        }
    }

    private ParsedPayload parse(JsonNode payload) {
        if (payload == null || !payload.isObject() || containsUnknownField(payload)) {
            invalid("Vehicle reading payload is invalid");
        }

        JsonNode typeNode = payload.get("readingType");
        JsonNode valueNode = payload.get("value");
        JsonNode recordedAtNode = payload.get("recordedAt");
        JsonNode notesNode = payload.get("notes");
        if (typeNode == null || !typeNode.isTextual() || valueNode == null || !valueNode.isNumber()
                || recordedAtNode == null || !recordedAtNode.isTextual()
                || (notesNode != null && !notesNode.isNull() && !notesNode.isTextual())) {
            invalid("Vehicle reading payload fields are invalid");
        }

        ManualVehicleReadingRecorder.ReadingType readingType;
        try {
            readingType = ManualVehicleReadingRecorder.ReadingType.valueOf(typeNode.textValue());
        } catch (IllegalArgumentException exception) {
            throw new OfflineSyncPayloadException("Reading type must be ODOMETER or ENGINE_HOURS");
        }

        BigDecimal value = valueNode.decimalValue();
        if (value.signum() < 0 || value.scale() > 3 || value.precision() > 19) {
            invalid("Reading value must be non-negative with at most three decimal places");
        }

        OffsetDateTime recordedAt;
        try {
            recordedAt = OffsetDateTime.parse(recordedAtNode.textValue());
        } catch (DateTimeParseException exception) {
            throw new OfflineSyncPayloadException("Recorded time must be a valid ISO offset date-time");
        }

        String notes = notesNode == null || notesNode.isNull() ? null : notesNode.textValue().trim();
        if (notes != null && notes.length() > 1000) {
            invalid("Reading notes cannot exceed 1000 characters");
        }
        return new ParsedPayload(readingType, value, recordedAt, notes == null || notes.isBlank() ? null : notes);
    }

    private boolean containsUnknownField(JsonNode payload) {
        Set<String> fields = new HashSet<>();
        Iterator<String> names = payload.fieldNames();
        names.forEachRemaining(fields::add);
        return !ALLOWED_FIELDS.containsAll(fields);
    }

    private void invalid(String message) {
        throw new OfflineSyncPayloadException(message);
    }

    private record ParsedPayload(ManualVehicleReadingRecorder.ReadingType readingType, BigDecimal value,
                                 OffsetDateTime recordedAt, String notes) {
    }
}
