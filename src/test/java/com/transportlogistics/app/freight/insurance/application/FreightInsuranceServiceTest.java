package com.transportlogistics.app.freight.insurance.application;

import com.transportlogistics.app.freight.insurance.domain.ClaimStatus;
import com.transportlogistics.app.freight.insurance.domain.FreightInsuranceClaim;
import com.transportlogistics.app.freight.insurance.domain.FreightInsurancePolicy;
import com.transportlogistics.app.freight.insurance.domain.PolicyStatus;
import com.transportlogistics.app.freight.insurance.ports.inbound.FreightInsuranceUseCase;
import com.transportlogistics.app.freight.insurance.ports.outbound.FreightInsuranceClaimRepository;
import com.transportlogistics.app.freight.insurance.ports.outbound.FreightInsuranceNumberGenerator;
import com.transportlogistics.app.freight.insurance.ports.outbound.FreightInsurancePolicyRepository;
import com.transportlogistics.app.freight.insurance.ports.outbound.FreightInsuranceTransaction;
import com.transportlogistics.app.freight.order.ports.inbound.FreightOrderLookup;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FreightInsuranceServiceTest {

    @Mock
    private FreightInsurancePolicyRepository policyRepo;

    @Mock
    private FreightInsuranceClaimRepository claimRepo;

    @Mock
    private FreightInsuranceNumberGenerator numberGenerator;

    @Mock
    private FreightOrderLookup freightOrderLookup;

    @Mock
    private FreightInsuranceTransaction transactions;

    private final Clock clock = Clock.fixed(Instant.parse("2026-08-25T10:00:00Z"), ZoneOffset.UTC);
    private FreightInsuranceService service;

    private final UUID policyId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID claimId = UUID.randomUUID();
    private final OffsetDateTime now = OffsetDateTime.now(clock);

    @BeforeEach
    void setUp() {
        service = new FreightInsuranceService(policyRepo, claimRepo, numberGenerator, freightOrderLookup, transactions, clock);
        when(transactions.execute(any())).thenAnswer(invocation -> {
            Supplier<?> s = invocation.getArgument(0);
            return s.get();
        });
    }

    @Test
    @DisplayName("Associate policy creates policy and persists with generated number")
    void testAssociatePolicy() {
        when(freightOrderLookup.find(orderId)).thenReturn(Optional.of(new FreightOrderLookup.OrderReference(orderId, "FO-2026-000001", List.of())));
        when(numberGenerator.nextPolicyNumber()).thenReturn("POL-2026-000001");
        when(policyRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var cmd = new FreightInsuranceUseCase.AssociatePolicyCommand(
                orderId, null, "Allianz", "ALL_RISK",
                new BigDecimal("50000.00"), new BigDecimal("1000.00"), "USD",
                now.minusDays(5), now.plusDays(30)
        );

        FreightInsurancePolicy policy = service.associatePolicy(cmd, "manager");
        assertThat(policy.getPolicyNumber()).isEqualTo("POL-2026-000001");
        assertThat(policy.getCoverageAmount()).isEqualByComparingTo("50000.00");
        verify(policyRepo).save(any());
    }

    @Test
    @DisplayName("Create claim checks policy coverage and persists claim")
    void testCreateClaim() {
        FreightInsurancePolicy policy = new FreightInsurancePolicy(
                policyId, "POL-2026-000001", orderId, null, "Allianz", "ALL_RISK",
                new BigDecimal("50000.00"), new BigDecimal("1000.00"), "USD",
                now.minusDays(5), now.plusDays(30), PolicyStatus.ACTIVE,
                now, now, "manager", "manager", 0L
        );
        when(policyRepo.findById(policyId)).thenReturn(Optional.of(policy));
        when(numberGenerator.nextClaimNumber()).thenReturn("CLM-2026-000001");
        when(claimRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var cmd = new FreightInsuranceUseCase.CreateClaimCommand(
                policyId, "INC-01", "Water leakage damage", new BigDecimal("12000.00")
        );

        FreightInsuranceClaim claim = service.createClaim(cmd, "manager");
        assertThat(claim.getClaimNumber()).isEqualTo("CLM-2026-000001");
        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.OPEN);
        assertThat(claim.getClaimedAmount()).isEqualByComparingTo("12000.00");
    }

    @Test
    @DisplayName("Assess claim rejects assessed amount greater than policy coverage")
    void testAssessClaimRejectsExcessiveAmount() {
        FreightInsurancePolicy policy = new FreightInsurancePolicy(
                policyId, "POL-2026-000001", orderId, null, "Allianz", "ALL_RISK",
                new BigDecimal("10000.00"), new BigDecimal("1000.00"), "USD",
                now.minusDays(5), now.plusDays(30), PolicyStatus.ACTIVE,
                now, now, "manager", "manager", 0L
        );
        FreightInsuranceClaim claim = new FreightInsuranceClaim(
                claimId, "CLM-2026-000001", policyId, orderId, "INC-01", "Damaged goods",
                new BigDecimal("8000.00"), null, null, null, null,
                ClaimStatus.OPEN, null, List.of(), now, now, "manager", "manager", 0L
        );

        when(claimRepo.findById(claimId)).thenReturn(Optional.of(claim));
        when(policyRepo.findById(policyId)).thenReturn(Optional.of(policy));

        var cmd = new FreightInsuranceUseCase.AssessClaimCommand(new BigDecimal("15000.00"), "Adjuster assessment", 0L);

        assertThatThrownBy(() -> service.assessClaim(claimId, cmd, "adjuster"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("exceeds policy coverage limit");
    }

    @Test
    @DisplayName("Concurrent update on claim throws ConflictException")
    void testClaimConcurrentUpdate() {
        FreightInsuranceClaim claim = new FreightInsuranceClaim(
                claimId, "CLM-2026-000001", policyId, orderId, "INC-01", "Damaged goods",
                new BigDecimal("8000.00"), null, null, null, null,
                ClaimStatus.OPEN, null, List.of(), now, now, "manager", "manager", 1L
        );
        when(claimRepo.findById(claimId)).thenReturn(Optional.of(claim));

        var cmd = new FreightInsuranceUseCase.ApproveClaimCommand(0L); // Stale version 0 vs 1

        assertThatThrownBy(() -> service.approveClaim(claimId, cmd, "manager"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("updated by another transaction");
    }
}
