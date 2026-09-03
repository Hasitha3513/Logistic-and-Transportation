package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.domain.model.*;
import com.transportlogistics.app.delivery.ports.outbound.ProofOfDeliveryRepository;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.UUID;

@Component
class ProofOfDeliveryPersistenceAdapter implements ProofOfDeliveryRepository {
    private final ProofOfDeliveryJpaRepository proofs; private final PodEvidenceJpaRepository evidence;
    ProofOfDeliveryPersistenceAdapter(ProofOfDeliveryJpaRepository proofs, PodEvidenceJpaRepository evidence) { this.proofs = proofs; this.evidence = evidence; }
    @Override public ProofOfDelivery save(ProofOfDelivery source) {
        ProofOfDeliveryEntity saved = proofs.saveAndFlush(toEntity(source));
        evidence.deleteByProofOfDeliveryId(source.id());
        evidence.flush();
        evidence.saveAllAndFlush(source.evidence().stream().map(item -> toEntity(source.id(), item)).toList());
        return toDomain(saved);
    }
    @Override public Optional<ProofOfDelivery> findByDeliveryOrderId(UUID id) { return proofs.findByDeliveryOrderId(id).map(this::toDomain); }
    private ProofOfDeliveryEntity toEntity(ProofOfDelivery p) {
        var e = new ProofOfDeliveryEntity(); e.setId(p.id()); e.setDeliveryOrderId(p.deliveryOrderId()); e.setStatus(p.status().name());
        e.setDeviceCapturedAt(p.deviceCapturedAt()); e.setLatitude(p.latitude()); e.setLongitude(p.longitude()); e.setAccuracyMeters(p.accuracyMeters());
        e.setSignerName(p.signerName()); e.setSignerRelationship(p.signerRelationship()); e.setAcceptedAt(p.acceptedAt()); e.setAcceptedBy(p.acceptedBy());
        e.setVersion(p.version()); e.setCreatedAt(p.createdAt()); e.setUpdatedAt(p.updatedAt()); e.setCreatedBy(p.createdBy()); e.setUpdatedBy(p.updatedBy()); return e;
    }
    private PodEvidenceEntity toEntity(UUID proofId, PodEvidence p) {
        var e = new PodEvidenceEntity(); e.setId(p.id()); e.setProofOfDeliveryId(proofId); e.setEvidenceType(p.type().name());
        e.setStorageReference(p.storageReference()); e.setBarcodeValue(p.barcodeValue()); e.setDetectedContentType(p.contentType());
        e.setContentLength(p.contentLength() == 0 ? null : p.contentLength()); e.setChecksum(p.checksum()); e.setOriginalFilename(p.originalFilename());
        e.setCaptureSource(p.captureSource()); e.setCreatedBy(p.createdBy()); e.setCreatedAt(p.createdAt()); return e;
    }
    private ProofOfDelivery toDomain(ProofOfDeliveryEntity p) {
        var items = evidence.findByProofOfDeliveryIdOrderByCreatedAt(p.getId()).stream().map(e -> new PodEvidence(e.getId(),
                PodEvidenceType.valueOf(e.getEvidenceType()), e.getStorageReference(), e.getBarcodeValue(), e.getDetectedContentType(),
                e.getContentLength() == null ? 0 : e.getContentLength(), e.getChecksum(), e.getOriginalFilename(), e.getCaptureSource(),
                e.getCreatedBy(), e.getCreatedAt())).toList();
        return new ProofOfDelivery(p.getId(), p.getDeliveryOrderId(), PodStatus.valueOf(p.getStatus()), p.getDeviceCapturedAt(),
                p.getLatitude(), p.getLongitude(), p.getAccuracyMeters(), p.getSignerName(), p.getSignerRelationship(),
                p.getAcceptedAt(), p.getAcceptedBy(), p.getVersion(), p.getCreatedAt(), p.getUpdatedAt(), p.getCreatedBy(), p.getUpdatedBy(), items);
    }
}
