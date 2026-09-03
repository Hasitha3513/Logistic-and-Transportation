package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

interface PodEvidenceJpaRepository extends JpaRepository<PodEvidenceEntity, UUID> {
    List<PodEvidenceEntity> findByProofOfDeliveryIdOrderByCreatedAt(UUID proofId);
    void deleteByProofOfDeliveryId(UUID proofId);
}
