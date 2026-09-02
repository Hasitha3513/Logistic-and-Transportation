package com.transportlogistics.app.delivery.adapters.inbound.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.delivery.domain.model.*;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryOrderUseCase;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DeliveryOrderApiSecurityAcceptanceTest {

    private static final UUID ID = UUID.fromString("03cd51bf-7ae3-44bd-8202-817fef87341d");
    private static final UUID CUSTOMER = UUID.fromString("f7df5124-4088-450d-8da8-cce83b9a0777");
    private static final UUID ORIGIN = UUID.fromString("5467daf8-cc62-438c-bbc8-a8316684821b");
    private static final UUID DESTINATION = UUID.fromString("66df281c-60fa-443d-b75b-cb47a523f8c3");

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @MockBean private DeliveryOrderUseCase orders;

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mvc.perform(get("/v1/deliveries")).andExpect(status().isUnauthorized());
        mvc.perform(post("/v1/deliveries").contentType(MediaType.APPLICATION_JSON).content(createBody()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void usersMissingExactDeliveryPermissionsAreForbidden() throws Exception {
        mvc.perform(get("/v1/deliveries")).andExpect(status().isForbidden());
        mvc.perform(post("/v1/deliveries").contentType(MediaType.APPLICATION_JSON).content(createBody()))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/v1/deliveries/{id}", ID).contentType(MediaType.APPLICATION_JSON).content(updateBody(0)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/v1/deliveries/{id}/validate-readiness", ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "DELIVERY_CREATE")
    void authorizedCreateReturnsGeneratedNumberAndValidatesPayload() throws Exception {
        when(orders.create(any(), eq("user"))).thenReturn(order(DeliveryStatus.DRAFT, 0));

        mvc.perform(post("/v1/deliveries").contentType(MediaType.APPLICATION_JSON).content(createBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deliveryNumber").value("DEL-2026-000001"));
        mvc.perform(post("/v1/deliveries").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "DELIVERY_CREATE")
    void clientSuppliedTenantIdCannotBecomeTenantAuthority() throws Exception {
        when(orders.create(any(), eq("user"))).thenReturn(order(DeliveryStatus.DRAFT, 0));
        var body = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(createBody());
        body.put("tenantId", UUID.randomUUID().toString());

        mvc.perform(post("/v1/deliveries").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isCreated());

        verify(orders).create(any(DeliveryOrderUseCase.CreateCommand.class), eq("user"));
    }

    @Test
    @WithMockUser(authorities = "DELIVERY_VIEW")
    void authorizedReadSupportsDetailListAndTenantSafeMissing() throws Exception {
        when(orders.get(ID)).thenReturn(order(DeliveryStatus.DRAFT, 0));
        when(orders.search(any())).thenReturn(new DeliveryOrderUseCase.PageResult<>(List.of(order(DeliveryStatus.DRAFT, 0)), 0, 20, 1, 1));

        mvc.perform(get("/v1/deliveries/{id}", ID)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()));
        mvc.perform(get("/v1/deliveries")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        var crossTenantId = UUID.randomUUID();
        when(orders.get(crossTenantId)).thenThrow(new NotFoundException("DELIVERY_NOT_FOUND", "Delivery Order not found"));
        mvc.perform(get("/v1/deliveries/{id}", crossTenantId)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DELIVERY_NOT_FOUND"));
    }

    @Test
    @WithMockUser(authorities = "DELIVERY_UPDATE")
    void authorizedUpdateSucceedsAndStaleVersionReturnsConflict() throws Exception {
        when(orders.update(eq(ID), any(), eq("user"))).thenReturn(order(DeliveryStatus.DRAFT, 1))
                .thenThrow(new ConflictException("DELIVERY_VERSION_CONFLICT", "Delivery Order was changed"));

        mvc.perform(patch("/v1/deliveries/{id}", ID).contentType(MediaType.APPLICATION_JSON).content(updateBody(0)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        mvc.perform(patch("/v1/deliveries/{id}", ID).contentType(MediaType.APPLICATION_JSON).content(updateBody(0)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DELIVERY_VERSION_CONFLICT"));
    }

    @Test
    @WithMockUser(authorities = "DELIVERY_ASSIGN")
    void authorizedReadinessSucceedsAndStaleVersionReturnsConflict() throws Exception {
        when(orders.markReady(ID, 0, "user")).thenReturn(order(DeliveryStatus.READY_FOR_ASSIGNMENT, 1))
                .thenThrow(new ConflictException("DELIVERY_VERSION_CONFLICT", "Delivery Order was changed"));

        mvc.perform(post("/v1/deliveries/{id}/validate-readiness", ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("READY_FOR_ASSIGNMENT"));
        mvc.perform(post("/v1/deliveries/{id}/validate-readiness", ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DELIVERY_VERSION_CONFLICT"));
    }

    @MockBean private com.transportlogistics.app.delivery.ports.inbound.ProofOfDeliveryUseCase proofs;
    @MockBean private com.transportlogistics.app.delivery.OfflineProofOfDeliveryRecorder podRecorder;

    @Test
    void unauthenticatedPodRequestsAreRejected() throws Exception {
        mvc.perform(get("/v1/deliveries/{id}/proof", ID)).andExpect(status().isUnauthorized());
        mvc.perform(post("/v1/deliveries/{id}/proof", ID).contentType(MediaType.APPLICATION_JSON).content("{\"deliveryVersion\":0}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/v1/deliveries/{id}/proof/finalize", ID).contentType(MediaType.APPLICATION_JSON).content("{\"deliveryVersion\":0,\"podVersion\":0}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void usersMissingPodPermissionsAreForbidden() throws Exception {
        mvc.perform(get("/v1/deliveries/{id}/proof", ID)).andExpect(status().isForbidden());
        mvc.perform(post("/v1/deliveries/{id}/proof", ID).contentType(MediaType.APPLICATION_JSON).content("{\"deliveryVersion\":0}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/v1/deliveries/{id}/proof/finalize", ID).contentType(MediaType.APPLICATION_JSON).content("{\"deliveryVersion\":0,\"podVersion\":0}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/v1/deliveries/{id}/proof/evidence/{evidenceId}?podVersion=0", ID, UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "DELIVERY_POD_CAPTURE")
    void authorizedPodCaptureFlowAndFinalize() throws Exception {
        var now = OffsetDateTime.parse("2026-08-29T10:00:00Z");
        var podId = UUID.randomUUID();
        var draft = new ProofOfDelivery(podId, ID, PodStatus.DRAFT, now, null, null, null, "John Doe", "Recipient", null, null, 0, now, now, "user", "user", List.of());
        when(proofs.create(eq(ID), any(), eq("user"))).thenReturn(draft);

        mvc.perform(post("/v1/deliveries/{id}/proof", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deliveryVersion\":0,\"signerName\":\"John Doe\",\"signerRelationship\":\"Recipient\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(podId.toString()))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        var finalized = new ProofOfDelivery(podId, ID, PodStatus.FINALIZED, now, null, null, null, "John Doe", "Recipient", now, "user", 1, now, now, "user", "user", List.of());
        var deliveredOrder = order(DeliveryStatus.DELIVERED, 1);
        when(proofs.finalizeProof(eq(ID), any(), eq("user"))).thenReturn(new com.transportlogistics.app.delivery.ports.inbound.ProofOfDeliveryUseCase.FinalizationResult(finalized, deliveredOrder));

        mvc.perform(post("/v1/deliveries/{id}/proof/finalize", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deliveryVersion\":0,\"podVersion\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proof.status").value("FINALIZED"))
                .andExpect(jsonPath("$.delivery.status").value("DELIVERED"));
    }

    @Test
    @WithMockUser(authorities = "DELIVERY_POD_VIEW")
    void authorizedPodViewAndContentStreaming() throws Exception {
        var now = OffsetDateTime.parse("2026-08-29T10:00:00Z");
        var podId = UUID.randomUUID();
        var evidenceId = UUID.randomUUID();
        var evidence = new PodEvidence(evidenceId, PodEvidenceType.PHOTO, "storage-ref-1", null, "image/png", 1024, "a".repeat(64), "photo.png", "FILE", "user", now);
        var proof = new ProofOfDelivery(podId, ID, PodStatus.FINALIZED, now, null, null, null, null, null, now, "user", 1, now, now, "user", "user", List.of(evidence));
        when(proofs.get(ID)).thenReturn(proof);

        mvc.perform(get("/v1/deliveries/{id}/proof", ID)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZED"))
                .andExpect(jsonPath("$.evidence[0].type").value("PHOTO"));

        when(proofs.content(ID, evidenceId)).thenReturn(new com.transportlogistics.app.delivery.ports.inbound.ProofOfDeliveryUseCase.EvidenceContent("test-image-bytes".getBytes(), "image/png", 16, "photo.png"));
        mvc.perform(get("/v1/deliveries/{id}/proof/evidence/{evidenceId}/content", ID, evidenceId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"));
    }

    private String createBody() throws Exception {
        return body(null);
    }

    private String updateBody(long version) throws Exception {
        return body(version);
    }

    private String body(Long version) throws Exception {
        var body = json.createObjectNode();
        if (version != null) body.put("version", version);
        body.put("customerId", CUSTOMER.toString());
        body.put("originLocationId", ORIGIN.toString());
        body.put("destinationLocationId", DESTINATION.toString());
        body.put("priority", "NORMAL");
        body.put("serviceType", "STANDARD");
        body.put("windowStart", "2026-08-30T10:00:00Z");
        body.put("windowEnd", "2026-08-31T10:00:00Z");
        body.put("instructions", "Handle carefully");
        return json.writeValueAsString(body);
    }

    private DeliveryOrder order(DeliveryStatus status, long version) {
        var now = OffsetDateTime.parse("2026-08-29T10:00:00Z");
        return new DeliveryOrder(new DeliveryId(ID), new DeliveryNumber("DEL-2026-000001"), CUSTOMER, ORIGIN,
                DESTINATION, DeliveryPriority.NORMAL, DeliveryServiceType.STANDARD,
                new DeliveryWindow(now.plusDays(1), now.plusDays(2)), "Handle carefully", status, version,
                now, now, "user", "user");
    }
}
