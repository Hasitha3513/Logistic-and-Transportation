package com.transportlogistics.app.delivery;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Narrow Delivery module boundary for recording offline Proof of Delivery. */
public interface OfflineProofOfDeliveryRecorder {
    Result recordOfflinePod(Command command);

    record Command(
            UUID deliveryId,
            long deliveryVersion,
            String signerName,
            String signerRelationship,
            boolean consentGiven,
            String consentVersion,
            OffsetDateTime consentTimestamp,
            OffsetDateTime deviceCapturedAt,
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal accuracyMeters,
            List<OfflineEvidenceItem> evidenceList,
            String actorUsername
    ) {
    }

    record OfflineEvidenceItem(
            String evidenceType,
            byte[] binaryContent,
            String barcodeValue,
            String captureSource,
            String originalFilename,
            String clientChecksum
    ) {
    }

    record Result(
            UUID podId,
            UUID deliveryId,
            String status,
            OffsetDateTime acceptedAt,
            String acceptedBy
    ) {
    }
}
