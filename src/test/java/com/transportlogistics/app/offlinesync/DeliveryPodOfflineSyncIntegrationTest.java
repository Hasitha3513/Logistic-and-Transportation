package com.transportlogistics.app.offlinesync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryOrderUseCase;
import com.transportlogistics.app.delivery.ports.inbound.ProofOfDeliveryUseCase;
import com.transportlogistics.app.offlinesync.application.ports.in.OfflineSyncUseCase;
import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncResult;
import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncResultStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "app.dev.identity-bootstrap.enabled=false",
        "app.dev.sample-data.enabled=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DeliveryPodOfflineSyncIntegrationTest {
    private static final OffsetDateTime RECORDED_AT = OffsetDateTime.parse("2026-08-30T10:00:00Z");

    @Autowired OfflineSyncUseCase offlineSync;
    @Autowired DeliveryOrderUseCase deliveryOrders;
    @Autowired ProofOfDeliveryUseCase podUseCase;
    @Autowired com.transportlogistics.app.tenancy.TenantContextExecutor tenantContexts;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    private UUID tenantId;
    private UUID actorId;
    private UUID customerId;
    private UUID originLocationId;
    private UUID destinationLocationId;
    private UUID deliveryId;
    private String deliveryNumber;
    private String username;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM offline_sync_operation");
        jdbc.update("DELETE FROM delivery_exception_evidence");
        jdbc.update("DELETE FROM delivery_exception_case");
        jdbc.update("DELETE FROM delivery_redelivery_schedule");
        jdbc.update("DELETE FROM delivery_escalation");
        jdbc.update("DELETE FROM delivery_contact_attempt");
        jdbc.update("DELETE FROM delivery_attempt");
        jdbc.update("DELETE FROM pod_evidence");
        jdbc.update("DELETE FROM proof_of_delivery");
        jdbc.update("DELETE FROM delivery_order");

        tenantId = com.transportlogistics.app.tenancy.CanonicalTenant.ID;
        actorId = UUID.randomUUID();
        customerId = UUID.fromString("f7df5124-4088-450d-8da8-cce83b9a0777");
        originLocationId = UUID.fromString("5467daf8-cc62-438c-bbc8-a8316684821b");
        destinationLocationId = UUID.fromString("66df281c-60fa-443d-b75b-cb47a523f8c3");
        deliveryId = UUID.randomUUID();
        deliveryNumber = "DEL-2026-999001";
        username = "offline-pod-" + actorId;

        jdbc.update("""
                INSERT INTO app_user (id, username, email, password_hash, first_name, last_name, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, actorId, username, actorId + "@test.local", "unused", "Offline", "Driver", true,
                RECORDED_AT, RECORDED_AT);

        jdbc.update("""
                INSERT INTO delivery_order (id, tenant_id, delivery_number, customer_id, origin_location_id,
                    destination_location_id, priority, service_type, window_start, window_end, instructions,
                    status, version, created_at, updated_at, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, 'NORMAL', 'STANDARD', ?, ?, 'Fragile', 'READY_FOR_ASSIGNMENT', 0, ?, ?, 'admin', 'admin')
                """, deliveryId, tenantId, deliveryNumber, customerId, originLocationId, destinationLocationId,
                RECORDED_AT, RECORDED_AT.plusHours(4), RECORDED_AT, RECORDED_AT);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM offline_sync_operation WHERE actor_id = ?", actorId);
        jdbc.update("DELETE FROM delivery_exception_evidence WHERE exception_case_id IN (SELECT id FROM delivery_exception_case WHERE delivery_order_id = ?)", deliveryId);
        jdbc.update("DELETE FROM delivery_exception_case WHERE delivery_order_id = ?", deliveryId);
        jdbc.update("DELETE FROM pod_evidence WHERE proof_of_delivery_id IN (SELECT id FROM proof_of_delivery WHERE delivery_order_id = ?)", deliveryId);
        jdbc.update("DELETE FROM proof_of_delivery WHERE delivery_order_id = ?", deliveryId);
        jdbc.update("DELETE FROM delivery_order WHERE id = ?", deliveryId);
        jdbc.update("DELETE FROM app_user WHERE id = ?", actorId);
    }

    @Test
    void appliesOfflineSignatureSyncAndCompletesDeliveryAtomically() throws Exception {
        UUID operationId = UUID.randomUUID();
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(10, 10, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", baos);
        String sigB64 = Base64.getEncoder().encodeToString(baos.toByteArray());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("deliveryId", deliveryId.toString());
        payload.put("deliveryVersion", 0);
        payload.put("signerName", "Alice Recipient");
        payload.put("signerRelationship", "Receiver");
        payload.put("consentGiven", true);
        payload.put("consentVersion", "POD-CONSENT-V1");
        payload.put("consentTimestamp", RECORDED_AT.toString());
        payload.put("deviceCapturedAt", RECORDED_AT.toString());
        payload.put("latitude", 6.9271);
        payload.put("longitude", 79.8612);
        payload.put("accuracyMeters", 5.0);
        payload.put("finalizeIntent", true);
        payload.put("evidenceList", List.of(Map.of(
                "evidenceType", "SIGNATURE",
                "binaryContent", sigB64,
                "captureSource", "MANUAL",
                "originalFilename", "sig.png"
        )));

        var command = command(operationId, deliveryId, payload);
        OfflineSyncResult result = sync(command, Set.of("DELIVERY_POD_CAPTURE"));

        assertEquals(OfflineSyncResultStatus.APPLIED, result.status());

        // Replay returns ALREADY_APPLIED idempotently
        OfflineSyncResult replayResult = sync(command, Set.of("DELIVERY_POD_CAPTURE"));
        assertEquals(OfflineSyncResultStatus.ALREADY_APPLIED, replayResult.status());

        // Verify Delivery is now DELIVERED in database
        String deliveryStatus = jdbc.queryForObject("SELECT status FROM delivery_order WHERE id = ?", String.class, deliveryId);
        assertEquals("DELIVERED", deliveryStatus);

        // Verify POD is FINALIZED in database
        String podStatus = jdbc.queryForObject("SELECT status FROM proof_of_delivery WHERE delivery_order_id = ?", String.class, deliveryId);
        assertEquals("FINALIZED", podStatus);

        Integer evidenceCount = jdbc.queryForObject("SELECT COUNT(*) FROM pod_evidence WHERE proof_of_delivery_id = (SELECT id FROM proof_of_delivery WHERE delivery_order_id = ?)", Integer.class, deliveryId);
        assertEquals(1, evidenceCount);
    }

    @Test
    void rejectsWhenConsentMissingAndClassifiesPermissionRevocation() {
        UUID opNoConsent = UUID.randomUUID();
        String sigB64 = Base64.getEncoder().encodeToString(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
        var payloadNoConsent = Map.of(
                "deliveryId", deliveryId.toString(),
                "deliveryVersion", 0,
                "signerName", "Alice Recipient",
                "consentGiven", false,
                "evidenceList", List.of(Map.of(
                        "evidenceType", "SIGNATURE",
                        "binaryContent", sigB64,
                        "originalFilename", "sig.png"
                ))
        );
        var resultNoConsent = sync(command(opNoConsent, deliveryId, payloadNoConsent), Set.of("DELIVERY_POD_CAPTURE"));
        assertEquals("POD_CONSENT_REQUIRED", resultNoConsent.errorCode());

        // When permission is missing/revoked
        UUID opForbidden = UUID.randomUUID();
        var payloadValid = Map.of(
                "deliveryId", deliveryId.toString(),
                "deliveryVersion", 0,
                "signerName", "Alice Recipient",
                "consentGiven", true,
                "consentVersion", "POD-CONSENT-V1",
                "evidenceList", List.of(Map.of(
                        "evidenceType", "BARCODE",
                        "barcodeValue", deliveryNumber
                ))
        );
        var resultForbidden = sync(command(opForbidden, deliveryId, payloadValid), Set.of());
        assertEquals("OFFLINE_SYNC_FORBIDDEN", resultForbidden.errorCode());
    }

    @Test
    void rejectsConcurrentDeliveryCompletionAsConflict() {
        // Mark delivery DELIVERED first
        jdbc.update("UPDATE delivery_order SET status = 'DELIVERED' WHERE id = ?", deliveryId);

        UUID operationId = UUID.randomUUID();
        var payload = Map.of(
                "deliveryId", deliveryId.toString(),
                "deliveryVersion", 0,
                "signerName", "Alice Recipient",
                "consentGiven", true,
                "consentVersion", "POD-CONSENT-V1",
                "evidenceList", List.of(Map.of(
                        "evidenceType", "BARCODE",
                        "barcodeValue", deliveryNumber
                ))
        );
        var result = sync(command(operationId, deliveryId, payload), Set.of("DELIVERY_POD_CAPTURE"));
        assertEquals(OfflineSyncResultStatus.CONFLICT, result.status());
    }

    private OfflineSyncUseCase.OperationCommand command(UUID operationId, UUID aggregateId, Map<String, Object> payloadMap) {
        return new OfflineSyncUseCase.OperationCommand(
                operationId, "DELIVERY_POD_OFFLINE_SYNC", 1, "DELIVERY", aggregateId,
                json.valueToTree(payloadMap), RECORDED_AT, RECORDED_AT, UUID.randomUUID(),
                operationId.toString(), null
        );
    }

    private OfflineSyncResult sync(OfflineSyncUseCase.OperationCommand command, Set<String> authorities) {
        return tenantContexts.within(new com.transportlogistics.app.tenancy.TenantExecutionContext(tenantId, actorId, username, UUID.randomUUID().toString()),
                () -> offlineSync.synchronize(new OfflineSyncUseCase.BatchCommand(username, authorities, List.of(command))).results().getFirst());
    }
}
