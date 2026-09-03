package com.transportlogistics.app.delivery.adapters.inbound.web.mappers;

import com.transportlogistics.app.delivery.adapters.inbound.web.dto.response.*;
import com.transportlogistics.app.delivery.domain.model.*;
import org.springframework.stereotype.Component;

@Component
public class ProofOfDeliveryWebMapper {
    public ProofOfDeliveryResponse toResponse(ProofOfDelivery proof) {
        return new ProofOfDeliveryResponse(proof.id(), proof.deliveryOrderId(), proof.status(), proof.deviceCapturedAt(),
                proof.latitude(), proof.longitude(), proof.accuracyMeters(), proof.signerName(), proof.signerRelationship(),
                proof.acceptedAt(), proof.acceptedBy(), proof.version(), proof.evidence().stream().map(this::toResponse).toList());
    }
    private PodEvidenceResponse toResponse(PodEvidence item) {
        return new PodEvidenceResponse(item.id(), item.type(), item.barcodeValue(), item.contentType(), item.contentLength(),
                item.checksum(), item.originalFilename(), item.captureSource(), item.createdBy(), item.createdAt());
    }
}
