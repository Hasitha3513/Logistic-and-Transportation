package com.transportlogistics.app.offlinesync.infrastructure.adapters.out.pod;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.delivery.OfflineProofOfDeliveryRecorder;
import com.transportlogistics.app.offlinesync.domain.model.OfflineOperationContext;
import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncPayloadException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DeliveryPodOfflineOperationHandlerTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void parsesValidPayloadAndDelegatesToRecorder() throws Exception {
        RecordingBoundary boundary = new RecordingBoundary();
        var handler = new DeliveryPodOfflineOperationHandler(boundary);
        UUID operationId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        String b64Content = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3, 4});

        var result = handler.apply(
                new OfflineOperationContext(operationId, actorId, "offline.driver", deliveryId,
                        OffsetDateTime.parse("2026-08-30T10:00:00Z")),
                json.readTree("""
                        {
                          "deliveryId": "%s",
                          "deliveryVersion": 0,
                          "signerName": "Alice Johnson",
                          "signerRelationship": "Recipient",
                          "consentGiven": true,
                          "consentVersion": "POD-CONSENT-V1",
                          "consentTimestamp": "2026-08-30T10:00:00Z",
                          "deviceCapturedAt": "2026-08-30T10:00:00Z",
                          "latitude": 6.9271,
                          "longitude": 79.8612,
                          "accuracyMeters": 10.5,
                          "finalizeIntent": true,
                          "evidenceList": [
                            {
                              "evidenceType": "SIGNATURE",
                              "binaryContent": "%s",
                              "captureSource": "MANUAL",
                              "originalFilename": "signature.png"
                            }
                          ]
                        }
                        """.formatted(deliveryId, b64Content))
        );

        assertEquals("APPLIED", result.status().name());
        assertEquals(deliveryId, boundary.command.deliveryId());
        assertEquals(0, boundary.command.deliveryVersion());
        assertEquals("Alice Johnson", boundary.command.signerName());
        assertEquals("Recipient", boundary.command.signerRelationship());
        assertTrue(boundary.command.consentGiven());
        assertEquals("POD-CONSENT-V1", boundary.command.consentVersion());
        assertEquals(1, boundary.command.evidenceList().size());
        assertEquals("SIGNATURE", boundary.command.evidenceList().get(0).evidenceType());
        assertArrayEquals(new byte[]{1, 2, 3, 4}, boundary.command.evidenceList().get(0).binaryContent());
        assertEquals("DELIVERY_POD_OFFLINE_SYNC", handler.operationType());
        assertEquals(1, handler.operationVersion());
        assertEquals(Set.of("DELIVERY_POD_CAPTURE"), handler.requiredAuthorities());
    }

    @Test
    void rejectsUnknownFieldsAndInvalidPayloads() {
        var handler = new DeliveryPodOfflineOperationHandler(new RecordingBoundary());
        var context = new OfflineOperationContext(UUID.randomUUID(), UUID.randomUUID(), "offline.driver", UUID.randomUUID(),
                OffsetDateTime.parse("2026-08-30T10:00:00Z"));

        assertThrows(OfflineSyncPayloadException.class, () -> handler.apply(context,
                json.readTree("{\"deliveryVersion\":0,\"unknown\":1,\"evidenceList\":[]}")));
        assertThrows(OfflineSyncPayloadException.class, () -> handler.apply(context,
                json.readTree("{\"deliveryVersion\":-1,\"evidenceList\":[]}")));
        assertThrows(OfflineSyncPayloadException.class, () -> handler.apply(context,
                json.readTree("{\"deliveryVersion\":0,\"evidenceList\":[]}")));
    }

    private static class RecordingBoundary implements OfflineProofOfDeliveryRecorder {
        OfflineProofOfDeliveryRecorder.Command command;

        @Override
        public Result recordOfflinePod(Command command) {
            this.command = command;
            return new Result(UUID.randomUUID(), command.deliveryId(), "FINALIZED",
                    OffsetDateTime.now(), command.actorUsername());
        }
    }
}
