package com.transportlogistics.app.delivery.ports.outbound;

import java.util.UUID;

public interface DeliveryEvidenceStoragePort {
    EvidenceReference referenceFor(UUID evidenceId);

    record EvidenceReference(UUID evidenceId, String evidenceType, String storageReference, String checksum) {}
}
