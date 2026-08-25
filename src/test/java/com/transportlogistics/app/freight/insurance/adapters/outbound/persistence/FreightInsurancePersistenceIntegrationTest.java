package com.transportlogistics.app.freight.insurance.adapters.outbound.persistence;

import com.transportlogistics.app.freight.insurance.domain.ClaimStatus;
import com.transportlogistics.app.freight.insurance.domain.FreightInsuranceClaim;
import com.transportlogistics.app.freight.insurance.domain.FreightInsurancePolicy;
import com.transportlogistics.app.freight.insurance.ports.inbound.FreightInsuranceUseCase;
import com.transportlogistics.app.freight.order.ports.inbound.FreightOrderUseCase;
import com.transportlogistics.app.shared.domain.ConflictException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class FreightInsurancePersistenceIntegrationTest {

    @Autowired
    private FreightInsuranceUseCase insuranceUseCase;

    @Autowired
    private FreightOrderUseCase freightOrderUseCase;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID customerId;
    private UUID originId;
    private UUID destinationId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        originId = UUID.randomUUID();
        destinationId = UUID.randomUUID();

        jdbc.update("INSERT INTO customer (id, code, name, active) VALUES (?, ?, ?, TRUE)",
                customerId, "CUST-INS-" + shortId(customerId), "Insurance Customer");
        jdbc.update("INSERT INTO location (id, code, name, active) VALUES (?, ?, ?, TRUE)",
                originId, "LOC-O-" + shortId(originId), "Origin Hub");
        jdbc.update("INSERT INTO location (id, code, name, active) VALUES (?, ?, ?, TRUE)",
                destinationId, "LOC-D-" + shortId(destinationId), "Destination Hub");

        var order = freightOrderUseCase.create(new FreightOrderUseCase.CreateCommand(
                customerId, originId, destinationId,
                OffsetDateTime.parse("2026-09-01T00:00:00Z"),
                OffsetDateTime.parse("2026-09-02T00:00:00Z"),
                "STANDARD", "NORMAL", null,
                List.of(new FreightOrderUseCase.LineCommand(null, "Electronic Turbines", BigDecimal.TEN))
        ), "planner");
        orderId = order.id();
    }

    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM freight_insurance_settlement");
        jdbc.update("DELETE FROM freight_insurance_claim");
        jdbc.update("DELETE FROM freight_insurance_policy");
        jdbc.update("DELETE FROM freight_order_line");
        jdbc.update("DELETE FROM freight_order");
        jdbc.update("DELETE FROM location WHERE id IN (?, ?)", originId, destinationId);
        jdbc.update("DELETE FROM customer WHERE id = ?", customerId);
    }

    @Test
    @DisplayName("Round-trip policy, claim, assessment, approval, and settlement history in database")
    void testPolicyAndClaimWorkflowPersistenceRoundTrip() {
        // 1. Associate policy
        var policyCmd = new FreightInsuranceUseCase.AssociatePolicyCommand(
                orderId, null, "Zurich Logistics", "ALL_RISK",
                new BigDecimal("100000.00"), new BigDecimal("2500.00"), "USD",
                OffsetDateTime.parse("2026-08-01T00:00:00Z"),
                OffsetDateTime.parse("2026-10-01T00:00:00Z")
        );
        FreightInsurancePolicy policy = insuranceUseCase.associatePolicy(policyCmd, "manager");
        assertThat(policy.getPolicyNumber()).startsWith("POL-");
        assertThat(policy.getCoverageAmount()).isEqualByComparingTo("100000.00");

        // 2. Create claim
        var claimCmd = new FreightInsuranceUseCase.CreateClaimCommand(
                policy.getId(), "INC-TURBINE-01", "Transit shock damage on turbine unit 1", new BigDecimal("45000.00")
        );
        FreightInsuranceClaim claim = insuranceUseCase.createClaim(claimCmd, "manager");
        assertThat(claim.getClaimNumber()).startsWith("CLM-");
        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.OPEN);

        // 3. Assess claim
        var assessCmd = new FreightInsuranceUseCase.AssessClaimCommand(
                new BigDecimal("40000.00"), "Assessed loss 40,000 USD after salvage deduction", claim.getVersion()
        );
        FreightInsuranceClaim assessed = insuranceUseCase.assessClaim(claim.getId(), assessCmd, "adjuster");
        assertThat(assessed.getStatus()).isEqualTo(ClaimStatus.UNDER_REVIEW);
        assertThat(assessed.getAssessedAmount()).isEqualByComparingTo("40000.00");

        // 4. Approve claim
        var approveCmd = new FreightInsuranceUseCase.ApproveClaimCommand(assessed.getVersion());
        FreightInsuranceClaim approved = insuranceUseCase.approveClaim(claim.getId(), approveCmd, "manager");
        assertThat(approved.getStatus()).isEqualTo(ClaimStatus.APPROVED);

        // 5. Partial settlement: 25,000
        var settle1Cmd = new FreightInsuranceUseCase.RecordSettlementCommand(
                new BigDecimal("25000.00"), "USD", "SETTLE-TR1", "First tranche", approved.getVersion()
        );
        FreightInsuranceClaim settled1 = insuranceUseCase.recordSettlement(claim.getId(), settle1Cmd, "finance");
        assertThat(settled1.getStatus()).isEqualTo(ClaimStatus.APPROVED);
        assertThat(settled1.getTotalSettledAmount()).isEqualByComparingTo("25000.00");
        assertThat(settled1.getRemainingApprovedAmount()).isEqualByComparingTo("15000.00");
        assertThat(settled1.getSettlements()).hasSize(1);

        // 6. Final settlement: 15,000
        var settle2Cmd = new FreightInsuranceUseCase.RecordSettlementCommand(
                new BigDecimal("15000.00"), "USD", "SETTLE-TR2", "Final tranche", settled1.getVersion()
        );
        FreightInsuranceClaim settled2 = insuranceUseCase.recordSettlement(claim.getId(), settle2Cmd, "finance");
        assertThat(settled2.getStatus()).isEqualTo(ClaimStatus.SETTLED);
        assertThat(settled2.getTotalSettledAmount()).isEqualByComparingTo("40000.00");
        assertThat(settled2.getRemainingApprovedAmount()).isEqualByComparingTo("0.00");
        assertThat(settled2.getSettlements()).hasSize(2);

        // 7. Verify reloading from database
        FreightInsuranceClaim reloaded = insuranceUseCase.getClaim(claim.getId());
        assertThat(reloaded.getStatus()).isEqualTo(ClaimStatus.SETTLED);
        assertThat(reloaded.getSettlements()).hasSize(2);
        assertThat(reloaded.getSettlements().getFirst().settlementReference()).isEqualTo("SETTLE-TR1");

        // 8. Stale update concurrency rejection
        var staleSettle = new FreightInsuranceUseCase.RecordSettlementCommand(
                new BigDecimal("1000.00"), "USD", "STALE", "Late retry", 0L
        );
        assertThatThrownBy(() -> insuranceUseCase.recordSettlement(claim.getId(), staleSettle, "finance"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("updated by another transaction");
    }

    private String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }
}
