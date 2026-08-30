package com.transportlogistics.app.offlinesync.infrastructure.adapters.out.pod;

import com.fasterxml.jackson.databind.JsonNode;
import com.transportlogistics.app.delivery.OfflineProofOfDeliveryRecorder;
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
import java.util.*;

@Component
final class DeliveryPodOfflineOperationHandler implements OfflineOperationHandler {
    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "deliveryId", "deliveryVersion", "signerName", "signerRelationship",
            "consentGiven", "consentVersion", "consentTimestamp", "deviceCapturedAt",
            "latitude", "longitude", "accuracyMeters", "evidenceList", "finalizeIntent"
    );
    private static final Set<String> ALLOWED_EVIDENCE_FIELDS = Set.of(
            "evidenceType", "binaryContent", "barcodeValue", "captureSource", "originalFilename", "clientChecksum"
    );

    private final OfflineProofOfDeliveryRecorder recorder;

    DeliveryPodOfflineOperationHandler(OfflineProofOfDeliveryRecorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public String operationType() {
        return "DELIVERY_POD_OFFLINE_SYNC";
    }

    @Override
    public int operationVersion() {
        return 1;
    }

    @Override
    public Set<String> requiredAuthorities() {
        return Set.of("DELIVERY_POD_CAPTURE");
    }

    @Override
    public OfflineHandlerOutcome apply(OfflineOperationContext context, JsonNode payload) {
        OfflineProofOfDeliveryRecorder.Command command = parse(context, payload);
        try {
            recorder.recordOfflinePod(command);
            return OfflineHandlerOutcome.applied();
        } catch (NotFoundException exception) {
            return OfflineHandlerOutcome.rejected(exception.code(), exception.getMessage());
        } catch (ConflictException exception) {
            throw new OfflineSyncConflictException(exception.getMessage());
        } catch (BusinessRuleException exception) {
            return OfflineHandlerOutcome.rejected(exception.code(), exception.getMessage());
        }
    }

    private OfflineProofOfDeliveryRecorder.Command parse(OfflineOperationContext context, JsonNode payload) {
        if (payload == null || !payload.isObject() || containsUnknownField(payload, ALLOWED_FIELDS)) {
            invalid("Delivery POD payload is invalid");
        }

        UUID deliveryId = context.aggregateId();
        if (payload.hasNonNull("deliveryId")) {
            try {
                UUID parsedId = UUID.fromString(payload.get("deliveryId").asText());
                if (!parsedId.equals(deliveryId)) {
                    invalid("deliveryId in payload does not match aggregateId");
                }
            } catch (IllegalArgumentException e) {
                invalid("deliveryId is not a valid UUID");
            }
        }

        JsonNode versionNode = payload.get("deliveryVersion");
        if (versionNode == null || !versionNode.isIntegralNumber() || versionNode.longValue() < 0) {
            invalid("deliveryVersion must be a non-negative integer");
        }
        long deliveryVersion = versionNode.longValue();

        String signerName = parseOptionalString(payload.get("signerName"), 200);
        String signerRelationship = parseOptionalString(payload.get("signerRelationship"), 100);

        boolean consentGiven = payload.hasNonNull("consentGiven") && payload.get("consentGiven").asBoolean();
        String consentVersion = parseOptionalString(payload.get("consentVersion"), 50);
        OffsetDateTime consentTimestamp = parseOptionalDateTime(payload.get("consentTimestamp"));
        OffsetDateTime deviceCapturedAt = parseOptionalDateTime(payload.get("deviceCapturedAt"));

        BigDecimal latitude = parseOptionalDecimal(payload.get("latitude"), new BigDecimal("-90"), new BigDecimal("90"));
        BigDecimal longitude = parseOptionalDecimal(payload.get("longitude"), new BigDecimal("-180"), new BigDecimal("180"));
        BigDecimal accuracyMeters = parseOptionalDecimal(payload.get("accuracyMeters"), BigDecimal.ZERO, new BigDecimal("100000"));

        JsonNode evidenceListNode = payload.get("evidenceList");
        if (evidenceListNode == null || !evidenceListNode.isArray() || evidenceListNode.isEmpty()) {
            invalid("evidenceList is required and must contain at least one evidence item");
        }

        List<OfflineProofOfDeliveryRecorder.OfflineEvidenceItem> evidenceItems = new ArrayList<>();
        for (JsonNode itemNode : evidenceListNode) {
            evidenceItems.add(parseEvidenceItem(itemNode));
        }

        return new OfflineProofOfDeliveryRecorder.Command(
                deliveryId, deliveryVersion, signerName, signerRelationship,
                consentGiven, consentVersion, consentTimestamp, deviceCapturedAt,
                latitude, longitude, accuracyMeters, evidenceItems, context.actorName()
        );
    }

    private OfflineProofOfDeliveryRecorder.OfflineEvidenceItem parseEvidenceItem(JsonNode node) {
        if (node == null || !node.isObject() || containsUnknownField(node, ALLOWED_EVIDENCE_FIELDS)) {
            invalid("Evidence item is invalid");
        }

        JsonNode typeNode = node.get("evidenceType");
        if (typeNode == null || !typeNode.isTextual() || typeNode.textValue().isBlank()) {
            invalid("evidenceType is required");
        }
        String evidenceType = typeNode.textValue().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("SIGNATURE", "PHOTO", "BARCODE").contains(evidenceType)) {
            invalid("evidenceType must be SIGNATURE, PHOTO, or BARCODE");
        }

        byte[] binaryContent = null;
        if (node.hasNonNull("binaryContent")) {
            String b64 = node.get("binaryContent").asText();
            try {
                binaryContent = Base64.getDecoder().decode(b64);
            } catch (IllegalArgumentException e) {
                invalid("binaryContent must be valid base64");
            }
        }

        String barcodeValue = parseOptionalString(node.get("barcodeValue"), 64);
        String captureSource = parseOptionalString(node.get("captureSource"), 20);
        String originalFilename = parseOptionalString(node.get("originalFilename"), 255);
        String clientChecksum = parseOptionalString(node.get("clientChecksum"), 64);

        return new OfflineProofOfDeliveryRecorder.OfflineEvidenceItem(
                evidenceType, binaryContent, barcodeValue, captureSource, originalFilename, clientChecksum
        );
    }

    private String parseOptionalString(JsonNode node, int maxLength) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isTextual()) {
            invalid("Field must be a string");
        }
        String text = node.textValue().trim();
        if (text.length() > maxLength) {
            invalid("Field exceeds maximum length of " + maxLength);
        }
        return text.isEmpty() ? null : text;
    }

    private OffsetDateTime parseOptionalDateTime(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isTextual()) {
            invalid("Field must be an ISO offset date-time string");
        }
        try {
            return OffsetDateTime.parse(node.textValue());
        } catch (DateTimeParseException e) {
            throw new OfflineSyncPayloadException("Field must be a valid ISO offset date-time");
        }
    }

    private BigDecimal parseOptionalDecimal(JsonNode node, BigDecimal min, BigDecimal max) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isNumber()) {
            invalid("Field must be a number");
        }
        BigDecimal val = node.decimalValue();
        if (min != null && val.compareTo(min) < 0) {
            invalid("Field must be greater than or equal to " + min);
        }
        if (max != null && val.compareTo(max) > 0) {
            invalid("Field must be less than or equal to " + max);
        }
        return val;
    }

    private boolean containsUnknownField(JsonNode node, Set<String> allowed) {
        Set<String> fields = new HashSet<>();
        Iterator<String> names = node.fieldNames();
        names.forEachRemaining(fields::add);
        return !allowed.containsAll(fields);
    }

    private void invalid(String message) {
        throw new OfflineSyncPayloadException(message);
    }
}
