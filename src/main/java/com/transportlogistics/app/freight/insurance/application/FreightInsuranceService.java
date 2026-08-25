package com.transportlogistics.app.freight.insurance.application;

import com.transportlogistics.app.freight.insurance.domain.FreightInsuranceClaim;
import com.transportlogistics.app.freight.insurance.domain.FreightInsurancePolicy;
import com.transportlogistics.app.freight.insurance.ports.inbound.FreightInsuranceUseCase;
import com.transportlogistics.app.freight.insurance.ports.outbound.FreightInsuranceClaimRepository;
import com.transportlogistics.app.freight.insurance.ports.outbound.FreightInsuranceNumberGenerator;
import com.transportlogistics.app.freight.insurance.ports.outbound.FreightInsurancePolicyRepository;
import com.transportlogistics.app.freight.insurance.ports.outbound.FreightInsuranceTransaction;
import com.transportlogistics.app.freight.order.ports.inbound.FreightOrderLookup;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class FreightInsuranceService implements FreightInsuranceUseCase {

    private final FreightInsurancePolicyRepository policyRepo;
    private final FreightInsuranceClaimRepository claimRepo;
    private final FreightInsuranceNumberGenerator numberGenerator;
    private final FreightOrderLookup freightOrderLookup;
    private final FreightInsuranceTransaction transactions;
    private final Clock clock;

    public FreightInsuranceService(FreightInsurancePolicyRepository policyRepo,
                                  FreightInsuranceClaimRepository claimRepo,
                                  FreightInsuranceNumberGenerator numberGenerator,
                                  FreightOrderLookup freightOrderLookup,
                                  FreightInsuranceTransaction transactions,
                                  Clock clock) {
        this.policyRepo = Objects.requireNonNull(policyRepo, "policyRepo is required");
        this.claimRepo = Objects.requireNonNull(claimRepo, "claimRepo is required");
        this.numberGenerator = Objects.requireNonNull(numberGenerator, "numberGenerator is required");
        this.freightOrderLookup = Objects.requireNonNull(freightOrderLookup, "freightOrderLookup is required");
        this.transactions = Objects.requireNonNull(transactions, "transactions is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    @Override
    public FreightInsurancePolicy associatePolicy(AssociatePolicyCommand command, String actor) {
        return transactions.execute(() -> {
            requireActor(actor);
            if (command.freightOrderId() == null) {
                throw new BusinessRuleException("FREIGHT_ORDER_ID_REQUIRED", "Freight order ID is required");
            }
            freightOrderLookup.find(command.freightOrderId())
                    .orElseThrow(() -> new NotFoundException("FREIGHT_ORDER_NOT_FOUND", "Freight order not found: " + command.freightOrderId()));

            OffsetDateTime now = OffsetDateTime.now(clock);
            String policyNumber = numberGenerator.nextPolicyNumber();

            FreightInsurancePolicy policy = new FreightInsurancePolicy(
                    UUID.randomUUID(),
                    policyNumber,
                    command.freightOrderId(),
                    command.cargoManifestId(),
                    command.insuranceProvider(),
                    command.policyType(),
                    command.coverageAmount(),
                    command.premiumAmount(),
                    command.currency(),
                    command.validFrom(),
                    command.validUntil(),
                    null,
                    now,
                    now,
                    actor,
                    actor,
                    0L
            );

            return policyRepo.save(policy);
        });
    }

    @Override
    public FreightInsurancePolicy getPolicy(UUID id) {
        return policyRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("INSURANCE_POLICY_NOT_FOUND", "Insurance policy not found: " + id));
    }

    @Override
    public List<FreightInsurancePolicy> listPolicies() {
        return policyRepo.findAll();
    }

    @Override
    public FreightInsurancePolicy updatePolicy(UUID id, UpdatePolicyCommand command, String actor) {
        return transactions.execute(() -> {
            requireActor(actor);
            FreightInsurancePolicy current = getPolicy(id);
            requireVersion(current.getVersion(), command.version());

            OffsetDateTime now = OffsetDateTime.now(clock);
            FreightInsurancePolicy updated = current.update(
                    command.insuranceProvider(),
                    command.policyType(),
                    command.coverageAmount(),
                    command.premiumAmount(),
                    command.validFrom(),
                    command.validUntil(),
                    command.status(),
                    actor,
                    now
            );

            return policyRepo.save(updated);
        });
    }

    @Override
    public FreightInsuranceClaim createClaim(CreateClaimCommand command, String actor) {
        return transactions.execute(() -> {
            requireActor(actor);
            if (command.policyId() == null) {
                throw new BusinessRuleException("POLICY_ID_REQUIRED", "Insurance policy ID is required for claim creation");
            }
            FreightInsurancePolicy policy = getPolicy(command.policyId());
            OffsetDateTime now = OffsetDateTime.now(clock);

            // Validate policy is active and coverage limit is not exceeded by initial claimed amount
            policy.validateCoverageFor(command.claimedAmount(), now);

            String claimNumber = numberGenerator.nextClaimNumber();

            FreightInsuranceClaim claim = new FreightInsuranceClaim(
                    UUID.randomUUID(),
                    claimNumber,
                    policy.getId(),
                    policy.getFreightOrderId(),
                    command.incidentReference(),
                    command.damageDescription(),
                    command.claimedAmount(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    now,
                    now,
                    actor,
                    actor,
                    0L
            );

            return claimRepo.save(claim);
        });
    }

    @Override
    public FreightInsuranceClaim getClaim(UUID id) {
        return claimRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("INSURANCE_CLAIM_NOT_FOUND", "Insurance claim not found: " + id));
    }

    @Override
    public List<FreightInsuranceClaim> listClaims() {
        return claimRepo.findAll();
    }

    @Override
    public FreightInsuranceClaim assessClaim(UUID id, AssessClaimCommand command, String actor) {
        return transactions.execute(() -> {
            requireActor(actor);
            FreightInsuranceClaim current = getClaim(id);
            requireVersion(current.getVersion(), command.version());

            FreightInsurancePolicy policy = getPolicy(current.getPolicyId());
            OffsetDateTime now = OffsetDateTime.now(clock);

            if (command.assessedAmount() != null && command.assessedAmount().compareTo(policy.getCoverageAmount()) > 0) {
                throw new BusinessRuleException("INSURANCE_COVERAGE_INSUFFICIENT",
                        "Assessed amount (" + command.assessedAmount() + ") exceeds policy coverage limit (" + policy.getCoverageAmount() + ")");
            }

            FreightInsuranceClaim assessed = current.assess(command.assessedAmount(), command.assessmentNotes(), actor, now);
            return claimRepo.save(assessed);
        });
    }

    @Override
    public FreightInsuranceClaim approveClaim(UUID id, ApproveClaimCommand command, String actor) {
        return transactions.execute(() -> {
            requireActor(actor);
            FreightInsuranceClaim current = getClaim(id);
            requireVersion(current.getVersion(), command.version());

            OffsetDateTime now = OffsetDateTime.now(clock);
            FreightInsuranceClaim approved = current.approve(actor, now);
            return claimRepo.save(approved);
        });
    }

    @Override
    public FreightInsuranceClaim rejectClaim(UUID id, RejectClaimCommand command, String actor) {
        return transactions.execute(() -> {
            requireActor(actor);
            FreightInsuranceClaim current = getClaim(id);
            requireVersion(current.getVersion(), command.version());

            OffsetDateTime now = OffsetDateTime.now(clock);
            FreightInsuranceClaim rejected = current.reject(command.reason(), actor, now);
            return claimRepo.save(rejected);
        });
    }

    @Override
    public FreightInsuranceClaim disputeClaim(UUID id, DisputeClaimCommand command, String actor) {
        return transactions.execute(() -> {
            requireActor(actor);
            FreightInsuranceClaim current = getClaim(id);
            requireVersion(current.getVersion(), command.version());

            OffsetDateTime now = OffsetDateTime.now(clock);
            FreightInsuranceClaim disputed = current.dispute(command.reason(), actor, now);
            return claimRepo.save(disputed);
        });
    }

    @Override
    public FreightInsuranceClaim recordSettlement(UUID id, RecordSettlementCommand command, String actor) {
        return transactions.execute(() -> {
            requireActor(actor);
            FreightInsuranceClaim current = getClaim(id);
            requireVersion(current.getVersion(), command.version());

            OffsetDateTime now = OffsetDateTime.now(clock);
            String settlementRef = command.settlementReference() != null && !command.settlementReference().isBlank()
                    ? command.settlementReference()
                    : "SETTLE-" + System.currentTimeMillis();

            FreightInsuranceClaim settled = current.recordSettlement(
                    UUID.randomUUID(),
                    settlementRef,
                    command.amount(),
                    command.currency(),
                    command.notes(),
                    actor,
                    now
            );

            return claimRepo.save(settled);
        });
    }

    private void requireVersion(long currentVersion, Long commandVersion) {
        if (commandVersion == null) {
            throw new BusinessRuleException("VERSION_REQUIRED", "Version is required");
        }
        if (currentVersion != commandVersion) {
            throw new ConflictException("INSURANCE_CONCURRENT_UPDATE", "Insurance entity was updated by another transaction; reload and retry");
        }
    }

    private void requireActor(String actor) {
        if (actor == null || actor.isBlank()) {
            throw new BusinessRuleException("ACTOR_REQUIRED", "An authenticated actor is required");
        }
    }
}
