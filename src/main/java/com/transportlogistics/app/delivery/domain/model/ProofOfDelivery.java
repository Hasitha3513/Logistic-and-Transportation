package com.transportlogistics.app.delivery.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record ProofOfDelivery(UUID id, UUID deliveryOrderId, PodStatus status, OffsetDateTime deviceCapturedAt,
                              BigDecimal latitude, BigDecimal longitude, BigDecimal accuracyMeters,
                              String signerName, String signerRelationship, OffsetDateTime acceptedAt,
                              String acceptedBy, long version, OffsetDateTime createdAt, OffsetDateTime updatedAt,
                              String createdBy, String updatedBy, List<PodEvidence> evidence) {
    public ProofOfDelivery {
        if (id == null || deliveryOrderId == null || status == null || version < 0 || createdAt == null || updatedAt == null) invalid("POD data is incomplete");
        validateGeo(latitude, longitude, accuracyMeters);
        signerName = normalize(signerName, 200, "Signer name"); signerRelationship = normalize(signerRelationship, 100, "Signer relationship");
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        createdBy = actor(createdBy); updatedBy = actor(updatedBy);
        if (status == PodStatus.FINALIZED && (acceptedAt == null || blank(acceptedBy))) invalid("Finalized POD acceptance data is incomplete");
    }
    public static ProofOfDelivery draft(UUID id, UUID deliveryId, OffsetDateTime deviceTime, BigDecimal latitude,
                                        BigDecimal longitude, BigDecimal accuracy, String signerName,
                                        String signerRelationship, OffsetDateTime now, String actor) {
        return new ProofOfDelivery(id, deliveryId, PodStatus.DRAFT, deviceTime, latitude, longitude, accuracy,
                signerName, signerRelationship, null, null, 0, now, now, actor, actor, List.of());
    }
    public ProofOfDelivery add(PodEvidence item, OffsetDateTime now, String actor) {
        requireDraft();
        long count = evidence.stream().filter(e -> e.type() == item.type()).count();
        int maximum = item.type() == PodEvidenceType.PHOTO ? 3 : 1;
        if (count >= maximum) throw new ConflictException("POD_EVIDENCE_LIMIT_EXCEEDED", "Evidence limit exceeded for " + item.type());
        var next = new ArrayList<>(evidence); next.add(item);
        return copy(next, now, actor, status, acceptedAt, acceptedBy);
    }
    public ProofOfDelivery remove(UUID evidenceId, OffsetDateTime now, String actor) {
        requireDraft(); var next = evidence.stream().filter(e -> !e.id().equals(evidenceId)).toList();
        if (next.size() == evidence.size()) throw new BusinessRuleException("POD_EVIDENCE_NOT_FOUND", "POD evidence was not found");
        return copy(next, now, actor, status, acceptedAt, acceptedBy);
    }
    public ProofOfDelivery finalizeAt(String expectedDeliveryNumber, OffsetDateTime now, String actor) {
        requireDraft();
        if (evidence.isEmpty()) invalid("At least one signature, photo or barcode is required");
        if (evidence.stream().anyMatch(e -> e.type() == PodEvidenceType.SIGNATURE) && blank(signerName)) invalid("Signer name is required for signature evidence");
        evidence.stream().filter(e -> e.type() == PodEvidenceType.BARCODE).forEach(e -> {
            if (!e.barcodeValue().equals(expectedDeliveryNumber)) throw new BusinessRuleException("POD_BARCODE_MISMATCH", "Barcode does not match the Delivery Order number");
        });
        return copy(evidence, now, actor, PodStatus.FINALIZED, now, actor);
    }
    private ProofOfDelivery copy(List<PodEvidence> next, OffsetDateTime now, String actor, PodStatus nextStatus,
                                 OffsetDateTime accepted, String accepter) {
        return new ProofOfDelivery(id, deliveryOrderId, nextStatus, deviceCapturedAt, latitude, longitude,
                accuracyMeters, signerName, signerRelationship, accepted, accepter, version, createdAt, now,
                createdBy, actor, next);
    }
    private void requireDraft() { if (status != PodStatus.DRAFT) throw new ConflictException("POD_ALREADY_FINALIZED", "Finalized POD evidence is immutable"); }
    private static void validateGeo(BigDecimal lat, BigDecimal lon, BigDecimal accuracy) {
        if ((lat == null) != (lon == null)) invalid("Latitude and longitude must be supplied together");
        if (lat != null && (lat.compareTo(BigDecimal.valueOf(-90)) < 0 || lat.compareTo(BigDecimal.valueOf(90)) > 0)) invalid("Latitude is outside the valid range");
        if (lon != null && (lon.compareTo(BigDecimal.valueOf(-180)) < 0 || lon.compareTo(BigDecimal.valueOf(180)) > 0)) invalid("Longitude is outside the valid range");
        if (accuracy != null && accuracy.signum() <= 0) invalid("Location accuracy must be positive");
    }
    private static String normalize(String value, int maximum, String label) {
        if (blank(value)) return null; String result = value.trim(); if (result.length() > maximum) invalid(label + " is too long"); return result;
    }
    private static String actor(String value) { if (blank(value)) invalid("Authenticated actor is required"); return value.trim(); }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static void invalid(String message) { throw new BusinessRuleException("POD_EVIDENCE_INVALID", message); }
}
