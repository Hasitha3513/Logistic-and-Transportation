package com.transportlogistics.app.delivery.ports.outbound;

import java.util.UUID;

public interface DeliveryEvidenceStoragePort {
    StoredEvidence store(UUID tenantId, UUID evidenceId, byte[] content, String originalFilename);
    StoredContent read(UUID tenantId, String storageReference);
    void delete(UUID tenantId, String storageReference);

    record StoredEvidence(String storageReference, String detectedContentType, long contentLength, String checksum) {}
    record StoredContent(byte[] content, String detectedContentType, long contentLength) {}
}
