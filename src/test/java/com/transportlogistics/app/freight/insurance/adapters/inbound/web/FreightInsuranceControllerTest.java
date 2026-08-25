package com.transportlogistics.app.freight.insurance.adapters.inbound.web;

import com.transportlogistics.app.freight.insurance.adapters.inbound.web.controllers.FreightInsuranceClaimController;
import com.transportlogistics.app.freight.insurance.adapters.inbound.web.controllers.FreightInsurancePolicyController;
import com.transportlogistics.app.freight.insurance.adapters.inbound.web.mappers.FreightInsuranceWebMapper;
import com.transportlogistics.app.freight.insurance.domain.ClaimStatus;
import com.transportlogistics.app.freight.insurance.domain.FreightInsuranceClaim;
import com.transportlogistics.app.freight.insurance.domain.FreightInsurancePolicy;
import com.transportlogistics.app.freight.insurance.domain.PolicyStatus;
import com.transportlogistics.app.freight.insurance.ports.inbound.FreightInsuranceUseCase;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FreightInsuranceControllerTest {

    private FreightInsuranceUseCase useCase;
    private MockMvc policyMvc;
    private MockMvc claimMvc;

    private final UUID policyId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID claimId = UUID.randomUUID();
    private final OffsetDateTime now = OffsetDateTime.parse("2026-08-25T10:00:00Z");

    @BeforeEach
    void setUp() {
        useCase = mock(FreightInsuranceUseCase.class);
        FreightInsuranceWebMapper mapper = new FreightInsuranceWebMapper();

        FreightInsurancePolicyController policyController = new FreightInsurancePolicyController(useCase, mapper);
        FreightInsuranceClaimController claimController = new FreightInsuranceClaimController(useCase, mapper);

        policyMvc = MockMvcBuilders.standaloneSetup(policyController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        claimMvc = MockMvcBuilders.standaloneSetup(claimController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /v1/freight/insurance/policies creates policy and returns 201")
    void testAssociatePolicySuccess() throws Exception {
        FreightInsurancePolicy policy = new FreightInsurancePolicy(
                policyId, "POL-2026-000001", orderId, null, "Allianz", "ALL_RISK",
                new BigDecimal("50000.00"), new BigDecimal("1000.00"), "USD",
                now.minusDays(1), now.plusDays(30), PolicyStatus.ACTIVE,
                now, now, "manager", "manager", 0L
        );
        when(useCase.associatePolicy(any(), any())).thenReturn(policy);

        policyMvc.perform(post("/v1/freight/insurance/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "freightOrderId": "%s",
                                  "insuranceProvider": "Allianz",
                                  "policyType": "ALL_RISK",
                                  "coverageAmount": 50000.00,
                                  "premiumAmount": 1000.00,
                                  "currency": "USD",
                                  "validFrom": "2026-08-24T10:00:00Z",
                                  "validUntil": "2026-09-24T10:00:00Z"
                                }
                                """.formatted(orderId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(policyId.toString()))
                .andExpect(jsonPath("$.policyNumber").value("POL-2026-000001"))
                .andExpect(jsonPath("$.coverageAmount").value(50000.00));
    }

    @Test
    @DisplayName("POST /v1/freight/insurance/claims creates claim and returns 201")
    void testCreateClaimSuccess() throws Exception {
        FreightInsuranceClaim claim = new FreightInsuranceClaim(
                claimId, "CLM-2026-000001", policyId, orderId, "INC-01", "Cargo damage",
                new BigDecimal("15000.00"), null, null, null, null,
                ClaimStatus.OPEN, null, List.of(),
                now, now, "manager", "manager", 0L
        );
        when(useCase.createClaim(any(), any())).thenReturn(claim);

        claimMvc.perform(post("/v1/freight/insurance/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "policyId": "%s",
                                  "incidentReference": "INC-01",
                                  "damageDescription": "Cargo damage",
                                  "claimedAmount": 15000.00
                                }
                                """.formatted(policyId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(claimId.toString()))
                .andExpect(jsonPath("$.claimNumber").value("CLM-2026-000001"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    @DisplayName("POST /v1/freight/insurance/claims/{id}/assess updates assessment")
    void testAssessClaimSuccess() throws Exception {
        FreightInsuranceClaim assessed = new FreightInsuranceClaim(
                claimId, "CLM-2026-000001", policyId, orderId, "INC-01", "Cargo damage",
                new BigDecimal("15000.00"), new BigDecimal("14000.00"), "Inspection verified",
                "adjuster", now, ClaimStatus.UNDER_REVIEW, null, List.of(),
                now, now, "manager", "manager", 0L
        );
        when(useCase.assessClaim(eq(claimId), any(), any())).thenReturn(assessed);

        claimMvc.perform(post("/v1/freight/insurance/claims/{id}/assess", claimId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assessedAmount": 14000.00,
                                  "assessmentNotes": "Inspection verified",
                                  "version": 0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assessedAmount").value(14000.00))
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));
    }

    @Test
    @DisplayName("POST /v1/freight/insurance/claims/{id}/settlements records settlement")
    void testRecordSettlementSuccess() throws Exception {
        FreightInsuranceClaim settled = new FreightInsuranceClaim(
                claimId, "CLM-2026-000001", policyId, orderId, "INC-01", "Cargo damage",
                new BigDecimal("15000.00"), new BigDecimal("14000.00"), "Inspection verified",
                "adjuster", now, ClaimStatus.APPROVED, null, List.of(),
                now, now, "manager", "manager", 0L
        );
        FreightInsuranceClaim postSettle = settled.recordSettlement(
                UUID.randomUUID(), "SETTLE-101", new BigDecimal("14000.00"), "USD", "Full payout", "finance", now
        );
        when(useCase.recordSettlement(eq(claimId), any(), any())).thenReturn(postSettle);

        claimMvc.perform(post("/v1/freight/insurance/claims/{id}/settlements", claimId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 14000.00,
                                  "currency": "USD",
                                  "settlementReference": "SETTLE-101",
                                  "notes": "Full payout",
                                  "version": 0
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SETTLED"))
                .andExpect(jsonPath("$.totalSettledAmount").value(14000.00));
    }

    @Test
    @DisplayName("Error mapping: 404 on not found, 409 on conflict")
    void testErrorHandling() throws Exception {
        when(useCase.getClaim(claimId)).thenThrow(new NotFoundException("INSURANCE_CLAIM_NOT_FOUND", "Claim not found"));

        claimMvc.perform(get("/v1/freight/insurance/claims/{id}", claimId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INSURANCE_CLAIM_NOT_FOUND"));

        when(useCase.approveClaim(eq(claimId), any(), any())).thenThrow(new ConflictException("INSURANCE_CLAIM_INVALID_STATE", "Cannot approve unassessed claim"));

        claimMvc.perform(post("/v1/freight/insurance/claims/{id}/approve", claimId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\": 0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSURANCE_CLAIM_INVALID_STATE"));
    }
}
