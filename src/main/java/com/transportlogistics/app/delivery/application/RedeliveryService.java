package com.transportlogistics.app.delivery.application;

import com.transportlogistics.app.delivery.DeliveryCustomerNotificationEvent;
import com.transportlogistics.app.delivery.domain.model.*;
import com.transportlogistics.app.delivery.ports.inbound.RedeliveryUseCase;
import com.transportlogistics.app.delivery.ports.outbound.*;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class RedeliveryService implements RedeliveryUseCase {

    private static final int MAX_CONCURRENT_CAPACITY = 50;
    private static final LocalTime DEPOT_OPEN = LocalTime.of(8, 0);
    private static final LocalTime DEPOT_CLOSE = LocalTime.of(20, 0);

    private final DeliveryOrderRepository orders;
    private final ProofOfDeliveryRepository proofs;
    private final DeliveryAttemptRepository attempts;
    private final DeliveryRedeliveryScheduleRepository schedules;
    private final DeliveryTenantContextPort tenantContext;
    private final DeliveryOrderTransaction transactions;
    private final DeliveryOrderEventPublisherPort eventPublisher;
    private final Clock clock;

    public RedeliveryService(
            DeliveryOrderRepository orders,
            ProofOfDeliveryRepository proofs,
            DeliveryAttemptRepository attempts,
            DeliveryRedeliveryScheduleRepository schedules,
            DeliveryTenantContextPort tenantContext,
            DeliveryOrderTransaction transactions,
            Clock clock
    ) {
        this(orders, proofs, attempts, schedules, tenantContext, transactions, null, clock);
    }

    public RedeliveryService(
            DeliveryOrderRepository orders,
            ProofOfDeliveryRepository proofs,
            DeliveryAttemptRepository attempts,
            DeliveryRedeliveryScheduleRepository schedules,
            DeliveryTenantContextPort tenantContext,
            DeliveryOrderTransaction transactions,
            DeliveryOrderEventPublisherPort eventPublisher,
            Clock clock
    ) {
        this.orders = orders;
        this.proofs = proofs;
        this.attempts = attempts;
        this.schedules = schedules;
        this.tenantContext = tenantContext;
        this.transactions = transactions;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    public List<RedeliverySuggestion> getSuggestions(UUID deliveryId, CustomerPreferenceInput preference) {
        var tenant = requiredTenant();
        ZoneId zoneId = resolveTenantZone(tenant);
        OffsetDateTime now = OffsetDateTime.now(clock);

        DeliveryOrder delivery = loadDelivery(deliveryId);
        validateEligibleForSuggestions(delivery);

        List<RedeliverySuggestion> suggestions = new ArrayList<>();

        // 1. If customer preference provided, evaluate preferred slot
        if (preference != null && preference.preferredStartTime() != null && preference.preferredEndTime() != null) {
            OffsetDateTime prefStart = preference.preferredStartTime();
            OffsetDateTime prefEnd = preference.preferredEndTime();

            boolean validPref = true;
            String note = "Customer preferred window";
            try {
                DeliveryRedeliverySchedule.validateWindowFutureAndHorizon(prefStart, prefEnd, now);
                validateBusinessHours(prefStart, prefEnd, zoneId);
            } catch (BusinessRuleException ex) {
                validPref = false;
                note = "Customer preference is outside allowed parameters: " + ex.getMessage();
            }

            if (validPref) {
                int count = schedules.countActiveOverlapping(tenant.tenantId(), prefStart, prefEnd, null);
                boolean available = count < MAX_CONCURRENT_CAPACITY;
                if (!available) {
                    note = "Preferred window capacity full (" + count + "/" + MAX_CONCURRENT_CAPACITY + " scheduled)";
                }
                suggestions.add(new RedeliverySuggestion(prefStart, prefEnd, "Preferred Window", available, note));
            }
        }

        // 2. Propose standard next-business-day morning and afternoon depot slots
        ZonedDateTime tenantNow = now.atZoneSameInstant(zoneId);
        LocalDate targetDate = tenantNow.toLocalDate().plusDays(1);
        if (tenantNow.toLocalTime().isAfter(DEPOT_CLOSE)) {
            targetDate = targetDate.plusDays(1);
        }

        // Morning Slot: 09:00 - 13:00
        OffsetDateTime morningStart = targetDate.atTime(9, 0).atZone(zoneId).toOffsetDateTime();
        OffsetDateTime morningEnd = targetDate.atTime(13, 0).atZone(zoneId).toOffsetDateTime();
        int morningCount = schedules.countActiveOverlapping(tenant.tenantId(), morningStart, morningEnd, null);
        boolean morningAvail = morningCount < MAX_CONCURRENT_CAPACITY;
        suggestions.add(new RedeliverySuggestion(
                morningStart, morningEnd, "Next-Day Morning (09:00 - 13:00)",
                morningAvail,
                morningAvail ? "Standard morning depot window" : "Slot capacity full (" + morningCount + "/" + MAX_CONCURRENT_CAPACITY + ")"
        ));

        // Afternoon Slot: 14:00 - 18:00
        OffsetDateTime afternoonStart = targetDate.atTime(14, 0).atZone(zoneId).toOffsetDateTime();
        OffsetDateTime afternoonEnd = targetDate.atTime(18, 0).atZone(zoneId).toOffsetDateTime();
        int afternoonCount = schedules.countActiveOverlapping(tenant.tenantId(), afternoonStart, afternoonEnd, null);
        boolean afternoonAvail = afternoonCount < MAX_CONCURRENT_CAPACITY;
        suggestions.add(new RedeliverySuggestion(
                afternoonStart, afternoonEnd, "Next-Day Afternoon (14:00 - 18:00)",
                afternoonAvail,
                afternoonAvail ? "Standard afternoon depot window" : "Slot capacity full (" + afternoonCount + "/" + MAX_CONCURRENT_CAPACITY + ")"
        ));

        return suggestions;
    }

    @Override
    public DeliveryRedeliverySchedule scheduleRedelivery(UUID deliveryId, ScheduleRedeliveryCommand command, String actor) {
        return transactions.execute(() -> {
            var tenant = requiredTenant();
            ZoneId zoneId = resolveTenantZone(tenant);
            OffsetDateTime now = OffsetDateTime.now(clock);

            DeliveryOrder delivery = loadDelivery(deliveryId);

            if (delivery.version() != command.expectedVersion()) {
                throw new ConflictException("DELIVERY_VERSION_CONFLICT", "Stale delivery order version");
            }

            validateEligibleForInitialSchedule(delivery);

            // Validate failed attempt link
            List<DeliveryAttempt> attemptHistory = attempts.findByDeliveryId(deliveryId);
            if (attemptHistory.isEmpty()) {
                throw new ConflictException("REDELIVERY_NOT_ELIGIBLE", "No delivery attempts recorded for order");
            }
            DeliveryAttempt latestAttempt = attemptHistory.get(attemptHistory.size() - 1);
            if (latestAttempt.disposition() != DeliveryFailureDisposition.REDELIVERY_ELIGIBLE) {
                throw new ConflictException("REDELIVERY_NOT_ELIGIBLE", "Latest attempt is not eligible for re-delivery");
            }
            if (command.failedAttemptId() != null && !command.failedAttemptId().equals(latestAttempt.id())) {
                throw new BusinessRuleException("INVALID_ATTEMPT_ID", "Referenced attempt does not match latest eligible attempt");
            }

            // Window and Business Hours validation
            DeliveryRedeliverySchedule.validateWindowFutureAndHorizon(command.scheduledStartTime(), command.scheduledEndTime(), now);
            validateBusinessHours(command.scheduledStartTime(), command.scheduledEndTime(), zoneId);

            // Transactional Capacity check
            int activeCount = schedules.countActiveOverlapping(tenant.tenantId(), command.scheduledStartTime(), command.scheduledEndTime(), null);
            if (activeCount >= MAX_CONCURRENT_CAPACITY) {
                throw new ConflictException("SLOT_CAPACITY_EXCEEDED", "Operational delivery slot capacity exceeded (" + activeCount + "/" + MAX_CONCURRENT_CAPACITY + " scheduled)");
            }

            // Persist confirmed schedule
            UUID scheduleId = UUID.randomUUID();
            DeliveryRedeliverySchedule schedule = DeliveryRedeliverySchedule.createConfirmed(
                    scheduleId,
                    tenant.tenantId(),
                    delivery.id(),
                    latestAttempt.id(),
                    command.schedulingMethod(),
                    command.preferredStartTime(),
                    command.preferredEndTime(),
                    command.customerPreferenceNotes(),
                    command.scheduledStartTime(),
                    command.scheduledEndTime(),
                    actor,
                    now
            );
            DeliveryRedeliverySchedule savedSchedule = schedules.save(schedule);

            // Mutate DeliveryOrder -> READY_FOR_ASSIGNMENT with new window
            DeliveryOrder updatedOrder = new DeliveryOrder(
                    delivery.id(),
                    delivery.deliveryNumber(),
                    delivery.customerId(),
                    delivery.originLocationId(),
                    delivery.destinationLocationId(),
                    delivery.priority(),
                    delivery.serviceType(),
                    new DeliveryWindow(command.scheduledStartTime(), command.scheduledEndTime()),
                    delivery.instructions(),
                    DeliveryStatus.READY_FOR_ASSIGNMENT,
                    delivery.version(),
                    delivery.createdAt(),
                    now,
                    delivery.createdBy(),
                    actor
            );
            orders.save(updatedOrder);
            publishScheduled(tenant.tenantId(), delivery, savedSchedule, now, actor);

            return savedSchedule;
        });
    }

    @Override
    public DeliveryRedeliverySchedule reschedule(UUID deliveryId, RescheduleRedeliveryCommand command, String actor) {
        return transactions.execute(() -> {
            var tenant = requiredTenant();
            ZoneId zoneId = resolveTenantZone(tenant);
            OffsetDateTime now = OffsetDateTime.now(clock);

            DeliveryOrder delivery = loadDelivery(deliveryId);

            if (delivery.version() != command.expectedVersion()) {
                throw new ConflictException("DELIVERY_VERSION_CONFLICT", "Stale delivery order version");
            }

            if (delivery.status() != DeliveryStatus.READY_FOR_ASSIGNMENT) {
                throw new ConflictException("RESCHEDULE_NOT_ELIGIBLE", "Order must be in READY_FOR_ASSIGNMENT to reschedule");
            }

            DeliveryRedeliverySchedule currentSchedule = schedules.findCurrentConfirmed(deliveryId)
                    .orElseThrow(() -> new ConflictException("NO_ACTIVE_SCHEDULE", "No active confirmed re-delivery schedule found to reschedule"));

            // Window and Business Hours validation
            DeliveryRedeliverySchedule.validateWindowFutureAndHorizon(command.scheduledStartTime(), command.scheduledEndTime(), now);
            validateBusinessHours(command.scheduledStartTime(), command.scheduledEndTime(), zoneId);

            // Transactional Capacity check excluding the superseded schedule
            int activeCount = schedules.countActiveOverlapping(tenant.tenantId(), command.scheduledStartTime(), command.scheduledEndTime(), currentSchedule.id());
            if (activeCount >= MAX_CONCURRENT_CAPACITY) {
                throw new ConflictException("SLOT_CAPACITY_EXCEEDED", "Operational delivery slot capacity exceeded (" + activeCount + "/" + MAX_CONCURRENT_CAPACITY + " scheduled)");
            }

            // Supersede current schedule
            currentSchedule.supersede(actor, command.supersedeReason(), now);
            schedules.save(currentSchedule);

            // Persist new confirmed schedule
            UUID newScheduleId = UUID.randomUUID();
            DeliveryRedeliverySchedule newSchedule = DeliveryRedeliverySchedule.createConfirmed(
                    newScheduleId,
                    tenant.tenantId(),
                    delivery.id(),
                    currentSchedule.deliveryAttemptId(),
                    RedeliverySchedulingMethod.AGENT_ASSISTED,
                    currentSchedule.preferredStartTime(),
                    currentSchedule.preferredEndTime(),
                    currentSchedule.customerPreferenceNotes(),
                    command.scheduledStartTime(),
                    command.scheduledEndTime(),
                    actor,
                    now
            );
            DeliveryRedeliverySchedule savedSchedule = schedules.save(newSchedule);

            // Update DeliveryOrder deliveryWindow
            DeliveryOrder updatedOrder = new DeliveryOrder(
                    delivery.id(),
                    delivery.deliveryNumber(),
                    delivery.customerId(),
                    delivery.originLocationId(),
                    delivery.destinationLocationId(),
                    delivery.priority(),
                    delivery.serviceType(),
                    new DeliveryWindow(command.scheduledStartTime(), command.scheduledEndTime()),
                    delivery.instructions(),
                    delivery.status(),
                    delivery.version(),
                    delivery.createdAt(),
                    now,
                    delivery.createdBy(),
                    actor
            );
            orders.save(updatedOrder);
            publishScheduled(tenant.tenantId(), delivery, savedSchedule, now, actor);

            return savedSchedule;
        });
    }

    private void publishScheduled(UUID tenantId, DeliveryOrder delivery,
                                  DeliveryRedeliverySchedule schedule, OffsetDateTime occurredAt, String actor) {
        if (eventPublisher == null) return;
        eventPublisher.publishEvent(DeliveryCustomerNotificationEvent.create("DELIVERY_REDELIVERY_SCHEDULED",
            tenantId, delivery.id().value(), occurredAt, java.util.Map.of(
                "deliveryNumber", delivery.deliveryNumber().value(),
                "customerId", delivery.customerId().toString(),
                "status", schedule.status().name(),
                "scheduleId", schedule.id().toString(),
                "scheduledWindowStart", schedule.scheduledStartTime().toString(),
                "scheduledWindowEnd", schedule.scheduledEndTime().toString(),
                "actor", actor)));
    }

    @Override
    public List<DeliveryRedeliverySchedule> getHistory(UUID deliveryId) {
        requiredTenant();
        loadDelivery(deliveryId);
        return schedules.findByDeliveryOrderId(deliveryId);
    }

    private void validateEligibleForSuggestions(DeliveryOrder delivery) {
        if (delivery.status() == DeliveryStatus.DELIVERED) {
            throw new ConflictException("DELIVERY_ALREADY_DELIVERED", "Cannot get suggestions for a delivered order");
        }
        if (delivery.status() == DeliveryStatus.RETURN_TO_BASE) {
            throw new ConflictException("DELIVERY_RETURNED_TO_BASE", "Cannot get suggestions for an order returned to base");
        }
        if (delivery.status() == DeliveryStatus.ESCALATED) {
            throw new ConflictException("DELIVERY_ESCALATED", "Cannot get suggestions for an order under escalation. Resolve escalation first.");
        }
        if (delivery.status() != DeliveryStatus.FAILED_ATTEMPT && delivery.status() != DeliveryStatus.READY_FOR_ASSIGNMENT) {
            throw new ConflictException("REDELIVERY_NOT_ELIGIBLE", "Only orders in FAILED_ATTEMPT or READY_FOR_ASSIGNMENT status can get slot suggestions");
        }

        proofs.findByDeliveryOrderId(delivery.id().value()).ifPresent(pod -> {
            if (pod.status() == PodStatus.FINALIZED) {
                throw new ConflictException("POD_ALREADY_FINALIZED", "Proof of delivery has already been finalized");
            }
        });
    }

    private void validateEligibleForInitialSchedule(DeliveryOrder delivery) {
        if (delivery.status() == DeliveryStatus.DELIVERED) {
            throw new ConflictException("DELIVERY_ALREADY_DELIVERED", "Cannot schedule re-delivery for a delivered order");
        }
        if (delivery.status() == DeliveryStatus.RETURN_TO_BASE) {
            throw new ConflictException("DELIVERY_RETURNED_TO_BASE", "Cannot schedule re-delivery for an order returned to base");
        }
        if (delivery.status() == DeliveryStatus.ESCALATED) {
            throw new ConflictException("DELIVERY_ESCALATED", "Cannot schedule re-delivery for an order under escalation. Resolve escalation first.");
        }
        if (delivery.status() != DeliveryStatus.FAILED_ATTEMPT) {
            throw new ConflictException("REDELIVERY_NOT_ELIGIBLE", "Only orders in FAILED_ATTEMPT status can be scheduled for re-delivery");
        }

        proofs.findByDeliveryOrderId(delivery.id().value()).ifPresent(pod -> {
            if (pod.status() == PodStatus.FINALIZED) {
                throw new ConflictException("POD_ALREADY_FINALIZED", "Proof of delivery has already been finalized");
            }
        });
    }

    private void validateBusinessHours(OffsetDateTime start, OffsetDateTime end, ZoneId zoneId) {
        ZonedDateTime zdtStart = start.atZoneSameInstant(zoneId);
        ZonedDateTime zdtEnd = end.atZoneSameInstant(zoneId);

        if (!zdtStart.toLocalDate().equals(zdtEnd.toLocalDate())) {
            throw new BusinessRuleException("OUT_OF_BUSINESS_HOURS", "Scheduled delivery window must be within a single operational business day");
        }

        LocalTime startTime = zdtStart.toLocalTime();
        LocalTime endTime = zdtEnd.toLocalTime();

        if (startTime.isBefore(DEPOT_OPEN) || endTime.isAfter(DEPOT_CLOSE)) {
            throw new BusinessRuleException("OUT_OF_BUSINESS_HOURS", "Scheduled delivery window must be within depot operational hours (08:00 to 20:00)");
        }
    }

    private DeliveryTenantContextPort.TenantContext requiredTenant() {
        return tenantContext.currentTenant()
                .orElseThrow(() -> new BusinessRuleException("TENANT_REQUIRED", "Tenant context is required"));
    }

    private ZoneId resolveTenantZone(DeliveryTenantContextPort.TenantContext tenant) {
        if (tenant.timeZone() != null && !tenant.timeZone().trim().isEmpty()) {
            try {
                return ZoneId.of(tenant.timeZone());
            } catch (Exception ignored) {
                // fall through
            }
        }
        return ZoneId.of("Asia/Colombo");
    }

    private DeliveryOrder loadDelivery(UUID deliveryId) {
        return orders.findById(deliveryId)
                .orElseThrow(() -> new NotFoundException("DELIVERY_NOT_FOUND", "Delivery order not found"));
    }
}
