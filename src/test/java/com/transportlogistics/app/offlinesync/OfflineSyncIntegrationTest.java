package com.transportlogistics.app.offlinesync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.offlinesync.application.ports.in.OfflineSyncUseCase;
import com.transportlogistics.app.offlinesync.application.ports.out.OfflineOperationHandler;
import com.transportlogistics.app.offlinesync.domain.model.OfflineHandlerOutcome;
import com.transportlogistics.app.offlinesync.domain.model.OfflineOperationContext;
import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncConflictException;
import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncPayloadException;
import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncResultStatus;
import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncRetryableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
@Import(OfflineSyncIntegrationTest.HandlerConfiguration.class)
class OfflineSyncIntegrationTest {
    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID ACTOR_TWO_ID = UUID.fromString("10000000-0000-4000-8000-000000000002");
    private static final UUID AGGREGATE_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");
    private static final UUID CLIENT_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-22T10:00:00Z");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired OfflineSyncUseCase useCase;
    @Autowired HandlerControl control;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.dev.identity-bootstrap.enabled", () -> "false");
        registry.add("app.dev.sample-data.enabled", () -> "false");
    }

    @BeforeEach
    void reset() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS offline_sync_test_mutation (operation_id UUID PRIMARY KEY, aggregate_id UUID NOT NULL)");
        jdbc.update("DELETE FROM offline_sync_test_mutation");
        jdbc.update("DELETE FROM offline_sync_operation");
        jdbc.update("DELETE FROM fuel_issue_history");
        jdbc.update("DELETE FROM fuel_issue");
        jdbc.update("DELETE FROM fuel_purchase_history");
        jdbc.update("DELETE FROM fuel_purchase");
        jdbc.update("DELETE FROM bunker_stock_adjustment");
        jdbc.update("DELETE FROM bunker_dip_reading");
        jdbc.update("DELETE FROM bunker_stock_movement");
        jdbc.update("DELETE FROM app_user_role");
        jdbc.update("DELETE FROM refresh_token");
        jdbc.update("DELETE FROM app_user");
        insertUser(ACTOR_ID, "offline.operator");
        insertUser(ACTOR_TWO_ID, "offline.other");
        control.reset();
    }

    @AfterEach
    void cleanUpOfflineSyncRows() {
        jdbc.update("DELETE FROM offline_sync_test_mutation");
        jdbc.update("DELETE FROM offline_sync_operation");
    }

    @Test
    void endpointRequiresAuthentication() throws Exception {
        mvc.perform(post("/offline-sync/operations").contentType(MediaType.APPLICATION_JSON)
                        .content(batch(operation(UUID.randomUUID(), "VEHICLE_READING_RECORD", Map.of("value", 1)))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedAllowedOperationAppliesAndReplaysWithoutSecondMutation() throws Exception {
        UUID operationId = UUID.randomUUID();
        String request = batch(operation(operationId, "VEHICLE_READING_RECORD", Map.of("value", 1)));

        mvc.perform(post("/offline-sync/operations").with(actor("VEHICLE_READING_CREATE"))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk()).andExpect(jsonPath("$.results[0].status").value("APPLIED"));
        mvc.perform(post("/offline-sync/operations").with(actor("VEHICLE_READING_CREATE"))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk()).andExpect(jsonPath("$.results[0].status").value("ALREADY_APPLIED"));

        assertEquals(1, count("offline_sync_test_mutation"));
        assertEquals(1, count("offline_sync_operation"));
    }

    @Test
    void batchBoundsAndInputOrderAreEnforced() throws Exception {
        List<Map<String, Object>> fiftyOne = new ArrayList<>();
        for (int index = 0; index < 51; index++) {
            fiftyOne.add(operation(UUID.randomUUID(), "VEHICLE_READING_RECORD", Map.of("value", index)));
        }
        mvc.perform(post("/offline-sync/operations").with(actor("VEHICLE_READING_CREATE"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("operations", fiftyOne))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OFFLINE_SYNC_BATCH_TOO_LARGE"));
        mvc.perform(post("/offline-sync/operations").with(actor("VEHICLE_READING_CREATE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"operations\":[]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        List<Map<String, Object>> fifty = new ArrayList<>();
        for (int index = 0; index < 50; index++) {
            fifty.add(operation(UUID.randomUUID(), "VEHICLE_READING_RECORD", Map.of("value", index)));
        }
        mvc.perform(post("/offline-sync/operations").with(actor("VEHICLE_READING_CREATE"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("operations", fifty))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.results.length()").value(50))
                .andExpect(jsonPath("$.results[0].operationId").value(fifty.getFirst().get("operationId").toString()))
                .andExpect(jsonPath("$.results[49].operationId").value(fifty.getLast().get("operationId").toString()));
    }

    @Test
    void perItemAuthorizationAndMixedPartialResultsUseCurrentAuthorities() throws Exception {
        var allowed = operation(UUID.randomUUID(), "VEHICLE_READING_RECORD", Map.of("value", 1));
        var forbidden = operation(UUID.randomUUID(), "TRIP_CHECKPOINT_RECORD", Map.of("checkpointType", "ARRIVAL"));

        mvc.perform(post("/offline-sync/operations").with(actor("VEHICLE_READING_CREATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("operations", List.of(allowed, forbidden)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("APPLIED"))
                .andExpect(jsonPath("$.results[1].status").value("REJECTED"))
                .andExpect(jsonPath("$.results[1].errorCode").value("OFFLINE_SYNC_FORBIDDEN"));

        var previouslyAllowed = command(UUID.randomUUID(), Map.of("value", 9));
        assertEquals(OfflineSyncResultStatus.APPLIED,
                sync("offline.operator", Set.of("VEHICLE_READING_CREATE"), previouslyAllowed).status());
        var afterRevocation = sync("offline.operator", Set.of(), previouslyAllowed);
        assertEquals("OFFLINE_SYNC_FORBIDDEN", afterRevocation.errorCode());
    }

    @Test
    void unsupportedTypeVersionAndInvalidIdempotencyAreItemResults() throws Exception {
        var unsupported = operation(UUID.randomUUID(), "UNSUPPORTED_RECORD", Map.of("description", "test"));
        var unsupportedVersion = operation(UUID.randomUUID(), "VEHICLE_READING_RECORD", Map.of("value", 1));
        unsupportedVersion.put("operationVersion", 2);
        var invalidKey = operation(UUID.randomUUID(), "VEHICLE_READING_RECORD", Map.of("value", 1));
        invalidKey.put("idempotencyKey", UUID.randomUUID().toString());

        mvc.perform(post("/offline-sync/operations").with(actor("VEHICLE_READING_CREATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("operations", List.of(unsupported, unsupportedVersion, invalidKey)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].errorCode").value("OFFLINE_SYNC_OPERATION_UNSUPPORTED"))
                .andExpect(jsonPath("$.results[1].errorCode").value("OFFLINE_SYNC_PAYLOAD_VERSION_UNSUPPORTED"))
                .andExpect(jsonPath("$.results[2].errorCode").value("OFFLINE_SYNC_PAYLOAD_INVALID"));
    }

    @Test
    void duplicateIdsWithinBatchReplayOrConflictDeterministically() throws Exception {
        UUID sameId = UUID.randomUUID();
        var first = operation(sameId, "VEHICLE_READING_RECORD", Map.of("value", 1));
        var identical = operation(sameId, "VEHICLE_READING_RECORD", Map.of("value", 1));
        mvc.perform(post("/offline-sync/operations").with(actor("VEHICLE_READING_CREATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("operations", List.of(first, identical)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("APPLIED"))
                .andExpect(jsonPath("$.results[1].status").value("ALREADY_APPLIED"));

        UUID changedId = UUID.randomUUID();
        var original = operation(changedId, "VEHICLE_READING_RECORD", Map.of("value", 1));
        var changed = operation(changedId, "VEHICLE_READING_RECORD", Map.of("value", 2));
        mvc.perform(post("/offline-sync/operations").with(actor("VEHICLE_READING_CREATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("operations", List.of(original, changed)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[1].status").value("CONFLICT"))
                .andExpect(jsonPath("$.results[1].errorCode").value("OFFLINE_SYNC_IDEMPOTENCY_MISMATCH"));
    }

    @Test
    void storedRejectionAndConflictReplayWithoutHandlerExecution() {
        var rejected = command(UUID.randomUUID(), Map.of("reject", true));
        var conflicted = command(UUID.randomUUID(), Map.of("conflict", true));

        assertEquals(OfflineSyncResultStatus.REJECTED, sync("offline.operator", Set.of("VEHICLE_READING_CREATE"), rejected).status());
        assertEquals(OfflineSyncResultStatus.REJECTED, sync("offline.operator", Set.of("VEHICLE_READING_CREATE"), rejected).status());
        assertEquals(OfflineSyncResultStatus.CONFLICT, sync("offline.operator", Set.of("VEHICLE_READING_CREATE"), conflicted).status());
        assertEquals(OfflineSyncResultStatus.CONFLICT, sync("offline.operator", Set.of("VEHICLE_READING_CREATE"), conflicted).status());
        assertEquals(2, control.invocations.get());
    }

    @Test
    void payloadAndActorMismatchNeverExecuteHandlerAgain() {
        UUID operationId = UUID.randomUUID();
        var original = command(operationId, Map.of("value", 1));
        var changed = command(operationId, Map.of("value", 2));
        assertEquals(OfflineSyncResultStatus.APPLIED, sync("offline.operator", Set.of("VEHICLE_READING_CREATE"), original).status());
        var payloadMismatch = sync("offline.operator", Set.of("VEHICLE_READING_CREATE"), changed);
        var actorMismatch = sync("offline.other", Set.of("VEHICLE_READING_CREATE"), original);

        assertEquals("OFFLINE_SYNC_IDEMPOTENCY_MISMATCH", payloadMismatch.errorCode());
        assertEquals("OFFLINE_SYNC_IDEMPOTENCY_MISMATCH", actorMismatch.errorCode());
        assertEquals(1, control.invocations.get());
    }

    @Test
    void payloadAndBusinessConflictExceptionsBecomeDurableTerminalResults() {
        var invalid = command(UUID.randomUUID(), Map.of("payloadInvalid", true));
        var conflict = command(UUID.randomUUID(), Map.of("throwConflict", true));
        assertEquals("OFFLINE_SYNC_PAYLOAD_INVALID",
                sync("offline.operator", Set.of("VEHICLE_READING_CREATE"), invalid).errorCode());
        assertEquals("OFFLINE_SYNC_CONFLICT",
                sync("offline.operator", Set.of("VEHICLE_READING_CREATE"), conflict).errorCode());
        assertEquals(2, count("offline_sync_operation"));
    }

    @Test
    void transientFailureRollsBackBusinessMutationAndInboxThenAllowsRetry() {
        UUID operationId = UUID.randomUUID();
        var transientCommand = command(operationId, Map.of("transient", true));
        assertEquals(OfflineSyncResultStatus.RETRYABLE_ERROR,
                sync("offline.operator", Set.of("VEHICLE_READING_CREATE"), transientCommand).status());
        assertEquals(0, count("offline_sync_operation"));
        assertEquals(0, count("offline_sync_test_mutation"));

        assertEquals(OfflineSyncResultStatus.APPLIED,
                sync("offline.operator", Set.of("VEHICLE_READING_CREATE"), command(operationId, Map.of("value", 3))).status());
        assertEquals(1, count("offline_sync_operation"));
        assertEquals(1, count("offline_sync_test_mutation"));
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void concurrentDuplicateExecutesHandlerAtMostOnce() throws Exception {
        UUID operationId = UUID.randomUUID();
        var operation = command(operationId, Map.of("block", true));
        control.block = true;
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> sync("offline.operator", Set.of("VEHICLE_READING_CREATE"), operation));
            assertTrue(control.entered.await(3, TimeUnit.SECONDS));
            var second = executor.submit(() -> sync("offline.operator", Set.of("VEHICLE_READING_CREATE"), operation));
            control.release.countDown();
            Set<OfflineSyncResultStatus> statuses = Set.of(first.get().status(), second.get().status());
            assertEquals(Set.of(OfflineSyncResultStatus.APPLIED, OfflineSyncResultStatus.ALREADY_APPLIED), statuses);
            assertEquals(1, control.invocations.get());
            assertEquals(1, count("offline_sync_test_mutation"));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void v29PersistsAuditFactsAndEnforcesConstraints() {
        var operation = command(UUID.randomUUID(), Map.of("value", 4));
        sync("offline.operator", Set.of("VEHICLE_READING_CREATE"), operation);
        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM offline_sync_operation WHERE operation_id = ?",
                operation.operationId());
        assertEquals(ACTOR_ID, row.get("ACTOR_ID"));
        assertEquals(AGGREGATE_ID, row.get("AGGREGATE_ID"));
        assertEquals(64, row.get("REQUEST_HASH").toString().length());
        assertEquals("APPLIED", row.get("RESULT_STATUS"));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES WHERE UPPER(TABLE_NAME) = 'OFFLINE_SYNC_OPERATION' AND UPPER(INDEX_NAME) LIKE 'IDX_OFFLINE_SYNC_%'", Integer.class));
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO offline_sync_operation (operation_id, operation_type, operation_version, actor_id, client_instance_id, aggregate_type, aggregate_id, request_hash, result_status, processed_at, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        UUID.randomUUID(), "VEHICLE_READING_RECORD", 0, ACTOR_ID, CLIENT_ID, "VEHICLE", AGGREGATE_ID,
                        "0".repeat(64), "APPLIED", NOW, NOW));
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO offline_sync_operation (operation_id, operation_type, operation_version, actor_id, client_instance_id, aggregate_type, aggregate_id, request_hash, result_status, processed_at, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        UUID.randomUUID(), "VEHICLE_READING_RECORD", 1, UUID.randomUUID(), CLIENT_ID, "VEHICLE", AGGREGATE_ID,
                        "0".repeat(64), "APPLIED", NOW, NOW));
    }

    private OfflineSyncUseCase.OperationCommand command(UUID operationId, Map<String, Object> payload) {
        return new OfflineSyncUseCase.OperationCommand(operationId, "TRIP_DELAY_RECORD", 1,
                "TRIP", AGGREGATE_ID, json.valueToTree(payload), NOW, NOW, CLIENT_ID,
                operationId.toString(), null);
    }

    private com.transportlogistics.app.offlinesync.domain.model.OfflineSyncResult sync(
            String username, Set<String> authorities, OfflineSyncUseCase.OperationCommand operation) {
        return useCase.synchronize(new OfflineSyncUseCase.BatchCommand(username, authorities, List.of(operation)))
                .results().getFirst();
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor actor(String... authorities) {
        return user("offline.operator").authorities(java.util.Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new).toArray(SimpleGrantedAuthority[]::new));
    }

    private Map<String, Object> operation(UUID operationId, String type, Map<String, Object> payload) {
        String effectiveType = type.equals("VEHICLE_READING_RECORD") ? "TRIP_DELAY_RECORD" : type;
        Map<String, Object> operation = new LinkedHashMap<>();
        operation.put("operationId", operationId.toString());
        operation.put("operationType", effectiveType);
        operation.put("operationVersion", 1);
        operation.put("aggregateType", effectiveType.startsWith("VEHICLE") ? "VEHICLE" : "TRIP");
        operation.put("aggregateId", AGGREGATE_ID.toString());
        operation.put("payload", payload);
        operation.put("clientCreatedAt", NOW.toString());
        operation.put("clientUpdatedAt", NOW.toString());
        operation.put("clientInstanceId", CLIENT_ID.toString());
        operation.put("idempotencyKey", operationId.toString());
        operation.put("baseVersion", null);
        return operation;
    }

    private String batch(Map<String, Object> operation) throws Exception {
        return json.writeValueAsString(Map.of("operations", List.of(operation)));
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private void insertUser(UUID id, String username) {
        jdbc.update("INSERT INTO app_user (id, username, email, password_hash, first_name, last_name, active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, username, username + "@example.test", "hash", "Offline", "User", true, NOW, NOW);
    }

    @TestConfiguration
    static class HandlerConfiguration {
        @Bean HandlerControl handlerControl() { return new HandlerControl(); }

        @Bean OfflineOperationHandler tripDelayOfflineOperationHandler(JdbcTemplate jdbc, HandlerControl control) {
            return handler("TRIP_DELAY_RECORD", "VEHICLE_READING_CREATE", jdbc, control);
        }

        @Bean OfflineOperationHandler tripCheckpointOfflineOperationHandler(JdbcTemplate jdbc, HandlerControl control) {
            return handler("TRIP_CHECKPOINT_RECORD", "TRIP_LOG_MANAGE", jdbc, control);
        }

        private OfflineOperationHandler handler(String type, String authority, JdbcTemplate jdbc, HandlerControl control) {
            return new OfflineOperationHandler() {
                @Override public String operationType() { return type; }
                @Override public int operationVersion() { return 1; }
                @Override public Set<String> requiredAuthorities() { return Set.of(authority); }
                @Override public OfflineHandlerOutcome apply(OfflineOperationContext context, JsonNode payload) {
                    control.invocations.incrementAndGet();
                    if (payload.path("payloadInvalid").asBoolean()) throw new OfflineSyncPayloadException("Payload invalid");
                    if (payload.path("throwConflict").asBoolean()) throw new OfflineSyncConflictException("Business conflict");
                    if (payload.path("reject").asBoolean()) return OfflineHandlerOutcome.rejected("TEST_REJECTED", "Rejected");
                    if (payload.path("conflict").asBoolean()) return OfflineHandlerOutcome.conflict("TEST_CONFLICT", "Conflict");
                    jdbc.update("INSERT INTO offline_sync_test_mutation (operation_id, aggregate_id) VALUES (?, ?)",
                            context.operationId(), context.aggregateId());
                    if (payload.path("transient").asBoolean()) throw new OfflineSyncRetryableException("Temporary failure");
                    if (control.block && payload.path("block").asBoolean()) {
                        control.entered.countDown();
                        try {
                            if (!control.release.await(5, TimeUnit.SECONDS)) {
                                throw new OfflineSyncRetryableException("Test release timed out");
                            }
                        } catch (InterruptedException exception) {
                            Thread.currentThread().interrupt();
                            throw new OfflineSyncRetryableException("Interrupted", exception);
                        }
                    }
                    return OfflineHandlerOutcome.applied();
                }
            };
        }
    }

    static class HandlerControl {
        private final AtomicInteger invocations = new AtomicInteger();
        private CountDownLatch entered = new CountDownLatch(1);
        private CountDownLatch release = new CountDownLatch(1);
        private volatile boolean block;

        void reset() {
            invocations.set(0);
            entered = new CountDownLatch(1);
            release = new CountDownLatch(1);
            block = false;
        }
    }
}
