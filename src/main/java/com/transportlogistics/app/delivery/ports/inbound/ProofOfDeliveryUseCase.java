package com.transportlogistics.app.delivery.ports.inbound;

import com.transportlogistics.app.delivery.domain.model.PodEvidenceType;
import com.transportlogistics.app.delivery.domain.model.ProofOfDelivery;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface ProofOfDeliveryUseCase {
    ProofOfDelivery create(UUID deliveryId, CreateCommand command, String actor);
    ProofOfDelivery get(UUID deliveryId);
    ProofOfDelivery addEvidence(UUID deliveryId, AddEvidenceCommand command, String actor);
    ProofOfDelivery removeEvidence(UUID deliveryId, UUID evidenceId, long version, String actor);
    FinalizationResult finalizeProof(UUID deliveryId, FinalizeCommand command, String actor);
    EvidenceContent content(UUID deliveryId, UUID evidenceId);

    record CreateCommand(long deliveryVersion, OffsetDateTime deviceCapturedAt, BigDecimal latitude,
                         BigDecimal longitude, BigDecimal accuracyMeters, String signerName,
                         String signerRelationship) {}
    record AddEvidenceCommand(long podVersion, PodEvidenceType type, byte[] content, String originalFilename,
                              String barcodeValue, String captureSource) {}
    record FinalizeCommand(long deliveryVersion, long podVersion) {}
    record FinalizationResult(ProofOfDelivery proof, com.transportlogistics.app.delivery.domain.model.DeliveryOrder delivery) {}
    record EvidenceContent(byte[] content, String contentType, long contentLength, String filename) {}
}
