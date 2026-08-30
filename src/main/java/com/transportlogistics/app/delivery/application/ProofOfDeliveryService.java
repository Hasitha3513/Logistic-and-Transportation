package com.transportlogistics.app.delivery.application;

import com.transportlogistics.app.delivery.domain.model.*;
import com.transportlogistics.app.delivery.ports.inbound.ProofOfDeliveryUseCase;
import com.transportlogistics.app.delivery.ports.outbound.*;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

public final class ProofOfDeliveryService implements ProofOfDeliveryUseCase, com.transportlogistics.app.delivery.OfflineProofOfDeliveryRecorder {
    private static final long SIGNATURE_LIMIT = 2L * 1024 * 1024;
    private static final long PHOTO_LIMIT = 10L * 1024 * 1024;
    private final DeliveryOrderRepository orders; private final ProofOfDeliveryRepository proofs;
    private final DeliveryEvidenceStoragePort storage; private final DeliveryTenantContextPort tenants;
    private final DeliveryOrderTransaction transactions; private final Clock clock;

    public ProofOfDeliveryService(DeliveryOrderRepository orders, ProofOfDeliveryRepository proofs,
                                  DeliveryEvidenceStoragePort storage, DeliveryTenantContextPort tenants,
                                  DeliveryOrderTransaction transactions, Clock clock) {
        this.orders = orders; this.proofs = proofs; this.storage = storage; this.tenants = tenants;
        this.transactions = transactions; this.clock = clock;
    }
    @Override public ProofOfDelivery create(UUID deliveryId, CreateCommand command, String actor) {
        return transactions.execute(() -> {
            requiredTenant(); DeliveryOrder delivery = delivery(deliveryId); requireVersion(command.deliveryVersion(), delivery.version(), "DELIVERY_VERSION_CONFLICT");
            if (delivery.status() != DeliveryStatus.READY_FOR_ASSIGNMENT) conflict("POD_DELIVERY_STATE_INELIGIBLE", "Delivery is not eligible for proof capture");
            if (proofs.findByDeliveryOrderId(deliveryId).isPresent()) conflict("POD_ALREADY_EXISTS", "A POD already exists for this Delivery Order");
            return proofs.save(ProofOfDelivery.draft(UUID.randomUUID(), deliveryId, command.deviceCapturedAt(),
                    command.latitude(), command.longitude(), command.accuracyMeters(), command.signerName(),
                    command.signerRelationship(), now(), actor));
        });
    }
    @Override public ProofOfDelivery get(UUID deliveryId) { requiredTenant(); delivery(deliveryId); return proof(deliveryId); }
    @Override public ProofOfDelivery addEvidence(UUID deliveryId, AddEvidenceCommand command, String actor) {
        UUID tenantId = requiredTenant(); DeliveryOrder delivery = delivery(deliveryId); ProofOfDelivery proof = proof(deliveryId);
        requireVersion(command.podVersion(), proof.version(), "POD_VERSION_CONFLICT");
        UUID evidenceId = UUID.randomUUID(); OffsetDateTime timestamp = now();
        if (command.type() == PodEvidenceType.BARCODE) {
            String value = normalizeBarcode(command.barcodeValue());
            if (!value.equals(delivery.deliveryNumber().value())) invalid("POD_BARCODE_MISMATCH", "Barcode does not match the Delivery Order number");
            return proofs.save(proof.add(new PodEvidence(evidenceId, command.type(), null, value, null, 0,
                    null, null, source(command.captureSource(), "MANUAL"), actor, timestamp), timestamp, actor));
        }
        byte[] content = command.content() == null ? new byte[0] : command.content();
        long limit = command.type() == PodEvidenceType.SIGNATURE ? SIGNATURE_LIMIT : PHOTO_LIMIT;
        if (content.length == 0 || content.length > limit) invalid("POD_FILE_TOO_LARGE", "Evidence file is empty or exceeds its size limit");
        var stored = storage.store(tenantId, evidenceId, content, command.originalFilename());
        if (!stored.detectedContentType().equals("image/png") && !stored.detectedContentType().equals("image/jpeg")) {
            storage.delete(tenantId, stored.storageReference()); invalid("POD_MEDIA_TYPE_UNSUPPORTED", "Only decoded PNG and JPEG evidence is accepted");
        }
        try {
            return proofs.save(proof.add(new PodEvidence(evidenceId, command.type(), stored.storageReference(), null,
                    stored.detectedContentType(), stored.contentLength(), stored.checksum(), command.originalFilename(),
                    source(command.captureSource(), "FILE"), actor, timestamp), timestamp, actor));
        } catch (RuntimeException error) { storage.delete(tenantId, stored.storageReference()); throw error; }
    }
    @Override public ProofOfDelivery removeEvidence(UUID deliveryId, UUID evidenceId, long version, String actor) {
        UUID tenantId = requiredTenant(); delivery(deliveryId); ProofOfDelivery proof = proof(deliveryId);
        requireVersion(version, proof.version(), "POD_VERSION_CONFLICT");
        var item = proof.evidence().stream().filter(e -> e.id().equals(evidenceId)).findFirst()
                .orElseThrow(() -> missing("POD_EVIDENCE_NOT_FOUND", "POD evidence was not found"));
        ProofOfDelivery saved = proofs.save(proof.remove(evidenceId, now(), actor));
        if (item.storageReference() != null) storage.delete(tenantId, item.storageReference());
        return saved;
    }
    @Override public FinalizationResult finalizeProof(UUID deliveryId, FinalizeCommand command, String actor) {
        return transactions.execute(() -> {
            UUID tenantId = requiredTenant(); DeliveryOrder delivery = delivery(deliveryId); ProofOfDelivery proof = proof(deliveryId);
            requireVersion(command.deliveryVersion(), delivery.version(), "DELIVERY_VERSION_CONFLICT");
            requireVersion(command.podVersion(), proof.version(), "POD_VERSION_CONFLICT");
            if (delivery.status() != DeliveryStatus.READY_FOR_ASSIGNMENT) conflict("POD_DELIVERY_STATE_INELIGIBLE", "Delivery is not eligible for POD finalization");
            proof.evidence().stream().filter(e -> e.storageReference() != null).forEach(e -> storage.read(tenantId, e.storageReference()));
            OffsetDateTime accepted = now();
            ProofOfDelivery finalized = proofs.save(proof.finalizeAt(delivery.deliveryNumber().value(), accepted, actor));
            DeliveryOrder completed = orders.save(delivery.markDelivered(accepted, actor));
            return new FinalizationResult(finalized, completed);
        });
    }
    @Override public EvidenceContent content(UUID deliveryId, UUID evidenceId) {
        UUID tenantId = requiredTenant(); delivery(deliveryId); ProofOfDelivery proof = proof(deliveryId);
        var item = proof.evidence().stream().filter(e -> e.id().equals(evidenceId) && e.storageReference() != null).findFirst()
                .orElseThrow(() -> missing("POD_EVIDENCE_NOT_FOUND", "POD evidence was not found"));
        var found = storage.read(tenantId, item.storageReference());
        return new EvidenceContent(found.content(), found.detectedContentType(), found.contentLength(), item.originalFilename());
    }

    @Override
    public com.transportlogistics.app.delivery.OfflineProofOfDeliveryRecorder.Result recordOfflinePod(
            com.transportlogistics.app.delivery.OfflineProofOfDeliveryRecorder.Command command) {
        return transactions.execute(() -> {
            UUID tenantId = requiredTenant();
            DeliveryOrder delivery = delivery(command.deliveryId());
            requireVersion(command.deliveryVersion(), delivery.version(), "DELIVERY_VERSION_CONFLICT");
            if (delivery.status() == DeliveryStatus.DELIVERED) {
                conflict("DELIVERY_ALREADY_DELIVERED", "Delivery has already been marked DELIVERED");
            }
            if (delivery.status() != DeliveryStatus.READY_FOR_ASSIGNMENT) {
                conflict("POD_DELIVERY_STATE_INELIGIBLE", "Delivery is not eligible for proof capture");
            }
            var existingPod = proofs.findByDeliveryOrderId(command.deliveryId());
            if (existingPod.isPresent() && existingPod.get().status() == PodStatus.FINALIZED) {
                conflict("POD_ALREADY_FINALIZED", "Proof of delivery has already been finalized");
            }

            var evidenceItems = command.evidenceList();
            if (evidenceItems == null || evidenceItems.isEmpty()) {
                invalid("POD_PRIMARY_EVIDENCE_REQUIRED", "At least one primary evidence is required");
            }

            boolean hasSignature = evidenceItems.stream().anyMatch(e -> "SIGNATURE".equalsIgnoreCase(e.evidenceType()));
            boolean hasPhoto = evidenceItems.stream().anyMatch(e -> "PHOTO".equalsIgnoreCase(e.evidenceType()));

            if ((hasSignature || hasPhoto) && (!command.consentGiven() || command.consentVersion() == null || command.consentVersion().isBlank())) {
                invalid("POD_CONSENT_REQUIRED", "Customer consent is required for signature or photo capture");
            }

            long signatureCount = evidenceItems.stream().filter(e -> "SIGNATURE".equalsIgnoreCase(e.evidenceType())).count();
            if (signatureCount > 1) {
                invalid("POD_EVIDENCE_INVALID", "Maximum 1 signature allowed per POD");
            }
            long photoCount = evidenceItems.stream().filter(e -> "PHOTO".equalsIgnoreCase(e.evidenceType())).count();
            if (photoCount > 3) {
                invalid("POD_EVIDENCE_INVALID", "Maximum 3 photos allowed per POD");
            }
            long barcodeCount = evidenceItems.stream().filter(e -> "BARCODE".equalsIgnoreCase(e.evidenceType())).count();
            if (barcodeCount > 1) {
                invalid("POD_EVIDENCE_INVALID", "Maximum 1 barcode allowed per POD");
            }

            if (hasSignature && (command.signerName() == null || command.signerName().trim().isEmpty())) {
                invalid("POD_SIGNER_NAME_REQUIRED", "Signer name is required when signature evidence is present");
            }

            OffsetDateTime timestamp = now();
            ProofOfDelivery draft = existingPod.orElseGet(() -> ProofOfDelivery.draft(
                    UUID.randomUUID(), command.deliveryId(), command.deviceCapturedAt(),
                    command.latitude(), command.longitude(), command.accuracyMeters(),
                    command.signerName(), command.signerRelationship(), timestamp, command.actorUsername()));

            ProofOfDelivery withEvidence = draft;
            for (var item : evidenceItems) {
                UUID evidenceId = UUID.randomUUID();
                if ("BARCODE".equalsIgnoreCase(item.evidenceType())) {
                    String value = normalizeBarcode(item.barcodeValue());
                    if (!value.equals(delivery.deliveryNumber().value())) {
                        invalid("POD_BARCODE_MISMATCH", "Barcode does not match the Delivery Order number");
                    }
                    withEvidence = withEvidence.add(new PodEvidence(evidenceId, PodEvidenceType.BARCODE, null, value, null, 0,
                            null, null, source(item.captureSource(), "SCANNER"), command.actorUsername(), timestamp), timestamp, command.actorUsername());
                } else if ("SIGNATURE".equalsIgnoreCase(item.evidenceType()) || "PHOTO".equalsIgnoreCase(item.evidenceType())) {
                    PodEvidenceType pType = "SIGNATURE".equalsIgnoreCase(item.evidenceType()) ? PodEvidenceType.SIGNATURE : PodEvidenceType.PHOTO;
                    byte[] content = item.binaryContent() == null ? new byte[0] : item.binaryContent();
                    long limit = pType == PodEvidenceType.SIGNATURE ? SIGNATURE_LIMIT : PHOTO_LIMIT;
                    if (content.length == 0 || content.length > limit) {
                        invalid("POD_FILE_TOO_LARGE", "Evidence file is empty or exceeds its size limit");
                    }
                    var stored = storage.store(tenantId, evidenceId, content, item.originalFilename());
                    if (!stored.detectedContentType().equals("image/png") && !stored.detectedContentType().equals("image/jpeg")) {
                        storage.delete(tenantId, stored.storageReference());
                        invalid("POD_MEDIA_TYPE_UNSUPPORTED", "Only decoded PNG and JPEG evidence is accepted");
                    }
                    withEvidence = withEvidence.add(new PodEvidence(evidenceId, pType, stored.storageReference(), null,
                            stored.detectedContentType(), stored.contentLength(), stored.checksum(), item.originalFilename(),
                            source(item.captureSource(), "FILE"), command.actorUsername(), timestamp), timestamp, command.actorUsername());
                } else {
                    invalid("POD_EVIDENCE_TYPE_UNSUPPORTED", "Unsupported evidence type: " + item.evidenceType());
                }
            }

            OffsetDateTime accepted = now();
            ProofOfDelivery finalized = proofs.save(withEvidence.finalizeAt(delivery.deliveryNumber().value(), accepted, command.actorUsername()));
            DeliveryOrder completed = orders.save(delivery.markDelivered(accepted, command.actorUsername()));

            return new com.transportlogistics.app.delivery.OfflineProofOfDeliveryRecorder.Result(
                    finalized.id(), completed.id().value(), finalized.status().name(), accepted, command.actorUsername());
        });
    }

    private UUID requiredTenant() { return tenants.currentTenant().orElseThrow(() -> invalidEx("TENANT_CONTEXT_REQUIRED", "An active Tenant context is required")).tenantId(); }
    private DeliveryOrder delivery(UUID id) { return orders.findById(id).orElseThrow(() -> missing("DELIVERY_NOT_FOUND", "Delivery Order was not found")); }
    private ProofOfDelivery proof(UUID id) { return proofs.findByDeliveryOrderId(id).orElseThrow(() -> missing("POD_NOT_FOUND", "Proof of Delivery was not found")); }
    private OffsetDateTime now() { return OffsetDateTime.now(clock); }
    private String normalizeBarcode(String value) { if (value == null || value.isBlank() || value.trim().length() > 64 || value.chars().anyMatch(Character::isISOControl)) invalid("POD_EVIDENCE_INVALID", "Barcode is invalid"); return value.trim().toUpperCase(Locale.ROOT); }
    private String source(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim().toUpperCase(Locale.ROOT); }
    private void requireVersion(long supplied, long current, String code) { if (supplied != current) conflict(code, "The resource changed; reload and retry"); }
    private static void invalid(String code, String message) { throw new BusinessRuleException(code, message); }
    private static BusinessRuleException invalidEx(String code, String message) { return new BusinessRuleException(code, message); }
    private static void conflict(String code, String message) { throw new ConflictException(code, message); }
    private static NotFoundException missing(String code, String message) { return new NotFoundException(code, message); }
}
