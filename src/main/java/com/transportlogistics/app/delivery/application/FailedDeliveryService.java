package com.transportlogistics.app.delivery.application;

import com.transportlogistics.app.delivery.domain.model.*;
import com.transportlogistics.app.delivery.ports.inbound.FailedDeliveryUseCase;
import com.transportlogistics.app.delivery.ports.outbound.*;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class FailedDeliveryService implements FailedDeliveryUseCase {
    private final DeliveryOrderRepository orders;
    private final ProofOfDeliveryRepository proofs;
    private final DeliveryAttemptRepository attempts;
    private final DeliveryContactAttemptRepository contactAttempts;
    private final DeliveryEscalationRepository escalations;
    private final DeliveryTenantContextPort tenantContext;
    private final DeliveryOrderTransaction transactions;
    private final Clock clock;

    public FailedDeliveryService(DeliveryOrderRepository orders,
                                 ProofOfDeliveryRepository proofs,
                                 DeliveryAttemptRepository attempts,
                                 DeliveryContactAttemptRepository contactAttempts,
                                 DeliveryEscalationRepository escalations,
                                 DeliveryTenantContextPort tenantContext,
                                 DeliveryOrderTransaction transactions,
                                 Clock clock) {
        this.orders = orders;
        this.proofs = proofs;
        this.attempts = attempts;
        this.contactAttempts = contactAttempts;
        this.escalations = escalations;
        this.tenantContext = tenantContext;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public DeliveryAttempt recordFailedAttempt(UUID deliveryId, RecordFailedAttemptCommand command, String actor) {
        return transactions.execute(() -> {
            requiredTenant();
            OffsetDateTime now = OffsetDateTime.now(clock);
            DeliveryOrder delivery = loadEligibleDelivery(deliveryId, command.expectedVersion());

            DeliveryFailureReason reason = command.failureReason();
            if (reason == null) {
                throw new BusinessRuleException("REASON_REQUIRED", "Failure reason is required");
            }
            reason.validateNotes(command.notes());

            DeliveryFailureDisposition disposition = reason.resolveDisposition(command.requestedDisposition());
            int nextAttemptNumber = attempts.countByDeliveryId(deliveryId) + 1;
            UUID attemptId = UUID.randomUUID();

            List<DeliveryContactAttempt> contacts = new ArrayList<>();
            if (command.contactAttempts() != null) {
                for (ContactAttemptInput input : command.contactAttempts()) {
                    contacts.add(DeliveryContactAttempt.create(
                            UUID.randomUUID(), attemptId, input.channel(),
                            input.contactTimestamp() == null ? now : input.contactTimestamp(),
                            input.outcome(), input.notes(), actor, now));
                }
            }

            DeliveryAttempt attempt = DeliveryAttempt.create(
                    attemptId, delivery.id(), nextAttemptNumber, command.attemptTimestamp(),
                    reason, command.notes(), disposition, contacts, actor, now);

            // Update delivery order status
            DeliveryOrder updatedOrder = delivery.recordFailedAttempt(disposition, now, actor);
            orders.save(updatedOrder);

            DeliveryAttempt savedAttempt = attempts.save(attempt);
            if (!contacts.isEmpty()) {
                contactAttempts.saveAll(contacts);
            }

            // If disposition is ESCALATED, automatically create escalation record
            if (disposition == DeliveryFailureDisposition.ESCALATED) {
                String escReason = command.notes() != null && !command.notes().isBlank()
                        ? command.notes()
                        : "Failed delivery escalated due to reason: " + reason.name();
                DeliveryEscalation escalation = DeliveryEscalation.create(
                        UUID.randomUUID(), delivery.id(), attemptId, escReason, actor, now);
                escalations.save(escalation);
            }

            return savedAttempt;
        });
    }

    @Override
    public DeliveryContactAttempt recordContactAttempt(UUID deliveryId, UUID attemptId,
                                                       RecordContactAttemptCommand command, String actor) {
        return transactions.execute(() -> {
            requiredTenant();
            OffsetDateTime now = OffsetDateTime.now(clock);
            DeliveryOrder delivery = loadDelivery(deliveryId);
            if (delivery.status() == DeliveryStatus.DELIVERED) {
                throw new ConflictException("DELIVERY_ALREADY_DELIVERED", "Cannot add contact attempt for a delivered order");
            }
            attempts.findById(attemptId).orElseThrow(() -> new NotFoundException("ATTEMPT_NOT_FOUND", "Delivery attempt was not found"));

            DeliveryContactAttempt contact = DeliveryContactAttempt.create(
                    UUID.randomUUID(), attemptId, command.channel(),
                    command.contactTimestamp() == null ? now : command.contactTimestamp(),
                    command.outcome(), command.notes(), actor, now);

            return contactAttempts.save(contact);
        });
    }

    @Override
    public DeliveryEscalation escalateDelivery(UUID deliveryId, EscalateDeliveryCommand command, String actor) {
        return transactions.execute(() -> {
            requiredTenant();
            OffsetDateTime now = OffsetDateTime.now(clock);
            DeliveryOrder delivery = loadEligibleDelivery(deliveryId, command.expectedVersion());

            if (command.reason() == null || command.reason().isBlank()) {
                throw new BusinessRuleException("ESCALATION_REASON_REQUIRED", "Escalation reason is required");
            }

            DeliveryOrder updatedOrder = delivery.escalate(now, actor);
            orders.save(updatedOrder);

            DeliveryEscalation escalation = DeliveryEscalation.create(
                    UUID.randomUUID(), delivery.id(), command.deliveryAttemptId(),
                    command.reason(), actor, now);

            return escalations.save(escalation);
        });
    }

    @Override
    public DeliveryEscalation updateEscalation(UUID deliveryId, UUID escalationId,
                                              UpdateEscalationCommand command, String actor) {
        return transactions.execute(() -> {
            requiredTenant();
            OffsetDateTime now = OffsetDateTime.now(clock);
            DeliveryOrder delivery = loadDelivery(deliveryId);
            DeliveryEscalation current = escalations.findById(escalationId)
                    .orElseThrow(() -> new NotFoundException("ESCALATION_NOT_FOUND", "Escalation record was not found"));

            DeliveryEscalation updated;
            if (command.status() == DeliveryEscalationStatus.RESOLVED) {
                updated = current.resolve(command.resolutionNotes(), actor, now);
                // Update delivery order status to next disposition if provided
                DeliveryFailureDisposition nextDisp = command.nextDisposition() == null
                        ? DeliveryFailureDisposition.REDELIVERY_ELIGIBLE
                        : command.nextDisposition();
                DeliveryOrder nextOrder = delivery.resolveEscalation(nextDisp, now, actor);
                orders.save(nextOrder);
            } else if (command.status() == DeliveryEscalationStatus.UNDER_REVIEW) {
                updated = current.underReview(actor, now);
            } else {
                updated = current;
            }

            return escalations.save(updated);
        });
    }

    @Override
    public DeliveryOrder initiateReturnToBase(UUID deliveryId, ReturnToBaseCommand command, String actor) {
        return transactions.execute(() -> {
            requiredTenant();
            OffsetDateTime now = OffsetDateTime.now(clock);
            DeliveryOrder delivery = loadEligibleDelivery(deliveryId, command.expectedVersion());

            DeliveryOrder updated = delivery.initiateReturnToBase(now, actor);
            return orders.save(updated);
        });
    }

    @Override
    public List<DeliveryAttempt> getAttemptHistory(UUID deliveryId) {
        requiredTenant();
        loadDelivery(deliveryId);
        List<DeliveryAttempt> list = attempts.findByDeliveryId(deliveryId);
        // Load contact attempts for each attempt
        List<DeliveryAttempt> enriched = new ArrayList<>();
        for (DeliveryAttempt att : list) {
            List<DeliveryContactAttempt> contacts = contactAttempts.findByDeliveryAttemptId(att.id());
            enriched.add(new DeliveryAttempt(att.id(), att.deliveryId(), att.attemptNumber(),
                    att.attemptTimestamp(), att.failureReason(), att.notes(), att.disposition(),
                    contacts, att.recordedBy(), att.recordedAt()));
        }
        return enriched;
    }

    @Override
    public List<DeliveryEscalation> getEscalations(UUID deliveryId) {
        requiredTenant();
        loadDelivery(deliveryId);
        return escalations.findByDeliveryId(deliveryId);
    }

    private DeliveryOrder loadDelivery(UUID deliveryId) {
        return orders.findById(deliveryId)
                .orElseThrow(() -> new NotFoundException("DELIVERY_NOT_FOUND", "Delivery order was not found"));
    }

    private DeliveryOrder loadEligibleDelivery(UUID deliveryId, long expectedVersion) {
        DeliveryOrder delivery = loadDelivery(deliveryId);
        if (delivery.status() == DeliveryStatus.DELIVERED) {
            throw new ConflictException("DELIVERY_ALREADY_DELIVERED", "Cannot record failure for a delivered order");
        }
        proofs.findByDeliveryOrderId(deliveryId).ifPresent(proof -> {
            if (proof.status() == PodStatus.FINALIZED) {
                throw new ConflictException("POD_ALREADY_FINALIZED", "Proof of delivery has already been finalized");
            }
        });
        if (delivery.version() != expectedVersion) {
            throw new ConflictException("DELIVERY_VERSION_CONFLICT", "Stale delivery order version");
        }
        return delivery;
    }

    private void requiredTenant() {
        tenantContext.currentTenant()
                .orElseThrow(() -> new BusinessRuleException("TENANT_CONTEXT_MISSING", "Tenant context is required"));
    }
}
