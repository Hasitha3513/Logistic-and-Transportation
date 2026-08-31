package com.transportlogistics.app.delivery.application;

import com.transportlogistics.app.delivery.domain.model.DeliveryOrder;
import com.transportlogistics.app.delivery.domain.model.DeliveryOrderRiderAssignment;
import com.transportlogistics.app.delivery.domain.model.DeliveryRider;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderAvailability;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderEvents;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderShift;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderShiftStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderType;
import com.transportlogistics.app.delivery.domain.model.DeliveryStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryZone;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneStatus;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryRiderUseCase;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryZoneLookupPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryOrderRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryOrderTransaction;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryRiderEventPublisherPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryRiderRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort;
import com.transportlogistics.app.delivery.ports.outbound.DriverEligibilityPort;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class DeliveryRiderService implements DeliveryRiderUseCase {

    private final DeliveryRiderRepository riderRepository;
    private final DeliveryOrderRepository orderRepository;
    private final DriverEligibilityPort driverEligibilityPort;
    private final DeliveryZoneLookupPort zoneLookupPort;
    private final DeliveryTenantContextPort tenantContext;
    private final DeliveryOrderTransaction transactions;
    private final DeliveryRiderEventPublisherPort eventPublisher;
    private final Clock clock;

    public DeliveryRiderService(
            DeliveryRiderRepository riderRepository,
            DeliveryOrderRepository orderRepository,
            DriverEligibilityPort driverEligibilityPort,
            DeliveryZoneLookupPort zoneLookupPort,
            DeliveryTenantContextPort tenantContext,
            DeliveryOrderTransaction transactions,
            DeliveryRiderEventPublisherPort eventPublisher,
            Clock clock
    ) {
        this.riderRepository = riderRepository;
        this.orderRepository = orderRepository;
        this.driverEligibilityPort = driverEligibilityPort;
        this.zoneLookupPort = zoneLookupPort;
        this.tenantContext = tenantContext;
        this.transactions = transactions;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    public DeliveryRider onboardRider(OnboardRiderCommand command, String actor) {
        UUID tenantId = requireTenantId();
        OffsetDateTime now = OffsetDateTime.now(clock);

        return transactions.execute(() -> {
            // Verify driver exists and is eligible
            var driverSummary = driverEligibilityPort.findDriver(command.driverId())
                    .orElseThrow(() -> new NotFoundException("DRIVER_NOT_FOUND", "Driver not found: " + command.driverId()));

            if (!driverSummary.active() || !"AVAILABLE".equalsIgnoreCase(driverSummary.status())) {
                throw new ConflictException("DELIVERY_RIDER_DRIVER_NOT_ELIGIBLE", "Driver is not active or available: " + command.driverId());
            }

            if (riderRepository.existsActiveByDriverId(command.driverId(), tenantId)) {
                throw new ConflictException("DELIVERY_RIDER_DRIVER_ALREADY_LINKED", "An active rider profile already exists for driver: " + command.driverId());
            }

            if (command.riderCode() != null && riderRepository.existsByRiderCode(command.riderCode(), tenantId)) {
                throw new ConflictException("DELIVERY_RIDER_CODE_DUPLICATE", "Rider code already exists: " + command.riderCode());
            }

            // Verify primary zone exists and is active
            DeliveryZone zone = zoneLookupPort.findZone(command.primaryZoneId())
                    .orElseThrow(() -> new NotFoundException("DELIVERY_ZONE_NOT_FOUND", "Primary delivery zone not found: " + command.primaryZoneId()));
            if (zone.status() != DeliveryZoneStatus.ACTIVE) {
                throw new ConflictException("DELIVERY_ZONE_INACTIVE", "Primary delivery zone is inactive");
            }

            String code = (command.riderCode() != null && !command.riderCode().isBlank())
                    ? command.riderCode().trim().toUpperCase()
                    : "RDR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            DeliveryRider rider = DeliveryRider.create(
                    UUID.randomUUID(),
                    tenantId,
                    code,
                    command.driverId(),
                    command.riderType(),
                    command.primaryZoneId(),
                    command.secondaryZoneIds(),
                    command.maxConcurrentDeliveries(),
                    actor != null ? actor : "system",
                    now
            );

            DeliveryRider saved = riderRepository.save(rider);
            if (eventPublisher != null) {
                eventPublisher.publishEvent(new DeliveryRiderEvents.DeliveryRiderCreatedEvent(
                        tenantId, saved.getId(), saved.getDriverId(), saved.getRiderCode(),
                        saved.getPrimaryZoneId(), saved.getRiderType(), saved.getCreatedAt(), actor
                ));
            }
            return saved;
        });
    }

    @Override
    public DeliveryRider updateRider(UUID id, UpdateRiderCommand command, String actor) {
        UUID tenantId = requireTenantId();
        OffsetDateTime now = OffsetDateTime.now(clock);

        return transactions.execute(() -> {
            DeliveryRider rider = riderRepository.findByIdForUpdate(id, tenantId)
                    .orElseThrow(() -> new NotFoundException("DELIVERY_RIDER_NOT_FOUND", "Delivery rider not found: " + id));

            // Validate primary zone
            DeliveryZone zone = zoneLookupPort.findZone(command.primaryZoneId())
                    .orElseThrow(() -> new NotFoundException("DELIVERY_ZONE_NOT_FOUND", "Primary delivery zone not found: " + command.primaryZoneId()));
            if (zone.status() != DeliveryZoneStatus.ACTIVE) {
                throw new ConflictException("DELIVERY_ZONE_INACTIVE", "Primary delivery zone is inactive");
            }

            rider.updateProfile(command.primaryZoneId(), command.secondaryZoneIds(), command.maxConcurrentDeliveries(), actor, now);
            return riderRepository.save(rider);
        });
    }

    @Override
    public DeliveryRider activateRider(UUID id, String actor) {
        UUID tenantId = requireTenantId();
        OffsetDateTime now = OffsetDateTime.now(clock);

        return transactions.execute(() -> {
            DeliveryRider rider = riderRepository.findByIdForUpdate(id, tenantId)
                    .orElseThrow(() -> new NotFoundException("DELIVERY_RIDER_NOT_FOUND", "Delivery rider not found: " + id));

            DeliveryRiderStatus prev = rider.getStatus();
            rider.activate(actor, now);
            DeliveryRider saved = riderRepository.save(rider);
            if (eventPublisher != null) {
                eventPublisher.publishEvent(new DeliveryRiderEvents.DeliveryRiderStatusChangedEvent(
                        tenantId, saved.getId(), prev, saved.getStatus(), now, actor
                ));
            }
            return saved;
        });
    }

    @Override
    public DeliveryRider deactivateRider(UUID id, String actor) {
        UUID tenantId = requireTenantId();
        OffsetDateTime now = OffsetDateTime.now(clock);

        return transactions.execute(() -> {
            DeliveryRider rider = riderRepository.findByIdForUpdate(id, tenantId)
                    .orElseThrow(() -> new NotFoundException("DELIVERY_RIDER_NOT_FOUND", "Delivery rider not found: " + id));

            int activeAssignments = riderRepository.countActiveAssignmentsForRider(id, tenantId);
            if (activeAssignments > 0) {
                throw new ConflictException("DELIVERY_RIDER_HAS_ACTIVE_ASSIGNMENTS", "Cannot deactivate rider with active delivery assignments (" + activeAssignments + ")");
            }

            DeliveryRiderStatus prev = rider.getStatus();
            rider.deactivate(actor, now);
            DeliveryRider saved = riderRepository.save(rider);
            if (eventPublisher != null) {
                eventPublisher.publishEvent(new DeliveryRiderEvents.DeliveryRiderStatusChangedEvent(
                        tenantId, saved.getId(), prev, saved.getStatus(), now, actor
                ));
            }
            return saved;
        });
    }

    @Override
    public DeliveryRider suspendRider(UUID id, String actor) {
        UUID tenantId = requireTenantId();
        OffsetDateTime now = OffsetDateTime.now(clock);

        return transactions.execute(() -> {
            DeliveryRider rider = riderRepository.findByIdForUpdate(id, tenantId)
                    .orElseThrow(() -> new NotFoundException("DELIVERY_RIDER_NOT_FOUND", "Delivery rider not found: " + id));

            DeliveryRiderStatus prev = rider.getStatus();
            rider.suspend(actor, now);
            DeliveryRider saved = riderRepository.save(rider);
            if (eventPublisher != null) {
                eventPublisher.publishEvent(new DeliveryRiderEvents.DeliveryRiderStatusChangedEvent(
                        tenantId, saved.getId(), prev, saved.getStatus(), now, actor
                ));
            }
            return saved;
        });
    }

    @Override
    public DeliveryRider getRider(UUID id) {
        UUID tenantId = requireTenantId();
        return riderRepository.findById(id, tenantId)
                .orElseThrow(() -> new NotFoundException("DELIVERY_RIDER_NOT_FOUND", "Delivery rider not found: " + id));
    }

    @Override
    public List<DeliveryRiderSummary> listRiders(UUID zoneId, DeliveryRiderStatus status, DeliveryRiderType riderType) {
        UUID tenantId = requireTenantId();
        LocalDate today = LocalDate.now(clock);

        List<DeliveryRider> riders = riderRepository.findAll(tenantId, zoneId, status, riderType);
        List<DeliveryRiderSummary> summaries = new ArrayList<>();

        for (DeliveryRider rider : riders) {
            var driverSummary = driverEligibilityPort.findDriver(rider.getDriverId()).orElse(null);
            int activeWorkload = riderRepository.countActiveAssignmentsForRider(rider.getId(), tenantId);
            List<DeliveryRiderShift> shiftsToday = riderRepository.findActiveShiftsByRiderIdAndDate(rider.getId(), today, tenantId);
            Optional<DeliveryRiderShift> currentShift = shiftsToday.stream()
                    .filter(s -> s.getStatus() == DeliveryRiderShiftStatus.ON_DUTY || s.getStatus() == DeliveryRiderShiftStatus.SCHEDULED)
                    .findFirst();

            DeliveryRiderAvailability availability = computeAvailability(rider, activeWorkload, currentShift);
            summaries.add(new DeliveryRiderSummary(
                    rider.getId(),
                    rider.getRiderCode(),
                    rider.getDriverId(),
                    driverSummary,
                    rider.getRiderType(),
                    rider.getStatus(),
                    availability,
                    rider.getPrimaryZoneId(),
                    rider.getSecondaryZoneIds(),
                    activeWorkload,
                    rider.getMaxConcurrentDeliveries(),
                    currentShift
            ));
        }
        return summaries;
    }

    @Override
    public DeliveryRiderShift createShift(UUID riderId, CreateShiftCommand command, String actor) {
        UUID tenantId = requireTenantId();
        OffsetDateTime now = OffsetDateTime.now(clock);

        return transactions.execute(() -> {
            // Lock rider to serialize shift creation
            DeliveryRider rider = riderRepository.findByIdForUpdate(riderId, tenantId)
                    .orElseThrow(() -> new NotFoundException("DELIVERY_RIDER_NOT_FOUND", "Delivery rider not found: " + riderId));

            if (rider.getStatus() != DeliveryRiderStatus.ACTIVE) {
                throw new ConflictException("DELIVERY_RIDER_INACTIVE", "Cannot create shift for inactive or suspended rider");
            }

            List<DeliveryRiderShift> existing = riderRepository.findActiveShiftsByRiderIdAndDate(riderId, command.shiftDate(), tenantId);
            boolean overlap = existing.stream().anyMatch(s -> s.overlapsWith(command.shiftDate(), command.startTime(), command.endTime()));
            if (overlap) {
                throw new ConflictException("DELIVERY_RIDER_SHIFT_OVERLAP", "An overlapping active or scheduled shift already exists for this rider on date " + command.shiftDate());
            }

            DeliveryRiderShift shift = DeliveryRiderShift.create(
                    UUID.randomUUID(),
                    tenantId,
                    riderId,
                    command.shiftDate(),
                    command.startTime(),
                    command.endTime(),
                    command.deliverySlotId(),
                    command.maxDeliveries(),
                    actor != null ? actor : "system",
                    now
            );

            return riderRepository.saveShift(shift);
        });
    }

    @Override
    public List<DeliveryRiderShift> listShifts(UUID riderId) {
        UUID tenantId = requireTenantId();
        return riderRepository.findShiftsByRiderId(riderId, tenantId);
    }

    @Override
    public DeliveryRiderShift updateDutyStatus(UUID riderId, UUID shiftId, DutyStatusCommand command, String actor) {
        UUID tenantId = requireTenantId();
        OffsetDateTime now = OffsetDateTime.now(clock);

        return transactions.execute(() -> {
            DeliveryRiderShift shift = riderRepository.findShiftById(shiftId, tenantId)
                    .orElseThrow(() -> new NotFoundException("DELIVERY_RIDER_SHIFT_NOT_FOUND", "Shift not found: " + shiftId));

            if (!shift.getRiderId().equals(riderId)) {
                throw new ConflictException("DELIVERY_RIDER_SHIFT_MISMATCH", "Shift does not belong to rider: " + riderId);
            }

            String action = command.action() != null ? command.action().trim().toUpperCase() : "";
            switch (action) {
                case "START_DUTY" -> shift.startDuty(actor, now);
                case "COMPLETE_DUTY" -> shift.completeDuty(actor, now);
                case "CANCEL_SHIFT" -> shift.cancelShift(actor, now);
                default -> throw new IllegalArgumentException("Unknown duty action: " + command.action());
            }

            return riderRepository.saveShift(shift);
        });
    }

    @Override
    public DeliveryOrderRiderAssignment assignRider(UUID deliveryOrderId, AssignRiderCommand command, String actor) {
        UUID tenantId = requireTenantId();
        OffsetDateTime now = OffsetDateTime.now(clock);

        return transactions.execute(() -> {
            // Lock delivery order first
            DeliveryOrder order = orderRepository.findByIdForUpdate(deliveryOrderId)
                    .orElseThrow(() -> new NotFoundException("DELIVERY_ORDER_NOT_FOUND", "Delivery order not found: " + deliveryOrderId));

            if (order.status() != DeliveryStatus.READY_FOR_ASSIGNMENT && order.status() != DeliveryStatus.FAILED_ATTEMPT) {
                throw new ConflictException("DELIVERY_ORDER_NOT_READY", "Delivery order is not eligible for assignment (status=" + order.status() + ")");
            }

            // Check if active assignment exists
            Optional<DeliveryOrderRiderAssignment> activeAssignment = riderRepository.findActiveAssignmentForOrder(deliveryOrderId, tenantId);
            if (activeAssignment.isPresent()) {
                throw new ConflictException("DELIVERY_RIDER_ALREADY_ASSIGNED", "Delivery order already has an active rider assignment");
            }

            // Lock Rider
            DeliveryRider rider = riderRepository.findByIdForUpdate(command.riderId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("DELIVERY_RIDER_NOT_FOUND", "Delivery rider not found: " + command.riderId()));

            validateRiderEligibility(rider, order, command.isOverride(), command.overrideReason());

            DeliveryOrderRiderAssignment assignment = DeliveryOrderRiderAssignment.create(
                    UUID.randomUUID(),
                    tenantId,
                    deliveryOrderId,
                    command.riderId(),
                    command.isOverride(),
                    command.overrideReason(),
                    actor != null ? actor : "system",
                    now
            );

            DeliveryOrderRiderAssignment saved = riderRepository.saveAssignment(assignment);

            if (eventPublisher != null) {
                eventPublisher.publishEvent(new DeliveryRiderEvents.DeliveryRiderAssignedEvent(
                        tenantId, deliveryOrderId, command.riderId(), saved.getId(), command.isOverride(), now, actor
                ));
            }

            return saved;
        });
    }

    @Override
    public DeliveryOrderRiderAssignment reassignRider(UUID deliveryOrderId, ReassignRiderCommand command, String actor) {
        UUID tenantId = requireTenantId();
        OffsetDateTime now = OffsetDateTime.now(clock);

        return transactions.execute(() -> {
            // Lock delivery order
            DeliveryOrder order = orderRepository.findByIdForUpdate(deliveryOrderId)
                    .orElseThrow(() -> new NotFoundException("DELIVERY_ORDER_NOT_FOUND", "Delivery order not found: " + deliveryOrderId));

            DeliveryOrderRiderAssignment currentAssignment = riderRepository.findActiveAssignmentForOrder(deliveryOrderId, tenantId)
                    .orElseThrow(() -> new ConflictException("DELIVERY_RIDER_NO_ACTIVE_ASSIGNMENT", "No active rider assignment found to reassign"));

            UUID oldRiderId = currentAssignment.getRiderId();
            UUID newRiderId = command.newRiderId();

            if (oldRiderId.equals(newRiderId)) {
                throw new ConflictException("DELIVERY_RIDER_SAME_REASSIGNMENT", "Cannot reassign to the same rider");
            }

            // Lock involved riders deterministically to prevent deadlocks
            UUID firstLock = oldRiderId.compareTo(newRiderId) < 0 ? oldRiderId : newRiderId;
            UUID secondLock = oldRiderId.compareTo(newRiderId) < 0 ? newRiderId : oldRiderId;

            riderRepository.findByIdForUpdate(firstLock, tenantId);
            riderRepository.findByIdForUpdate(secondLock, tenantId);

            DeliveryRider newRider = riderRepository.findById(newRiderId, tenantId)
                    .orElseThrow(() -> new NotFoundException("DELIVERY_RIDER_NOT_FOUND", "New delivery rider not found: " + newRiderId));

            validateRiderEligibility(newRider, order, command.isOverride(), command.overrideReason());

            // Close old assignment
            currentAssignment.reassign(actor, now);
            riderRepository.saveAssignment(currentAssignment);

            // Create new assignment
            DeliveryOrderRiderAssignment newAssignment = DeliveryOrderRiderAssignment.create(
                    UUID.randomUUID(),
                    tenantId,
                    deliveryOrderId,
                    newRiderId,
                    command.isOverride(),
                    command.overrideReason(),
                    actor != null ? actor : "system",
                    now
            );

            DeliveryOrderRiderAssignment saved = riderRepository.saveAssignment(newAssignment);

            if (eventPublisher != null) {
                eventPublisher.publishEvent(new DeliveryRiderEvents.DeliveryRiderReassignedEvent(
                        tenantId, deliveryOrderId, oldRiderId, newRiderId, saved.getId(), now, actor
                ));
            }

            return saved;
        });
    }

    @Override
    public void unassignRider(UUID deliveryOrderId, String actor) {
        UUID tenantId = requireTenantId();
        OffsetDateTime now = OffsetDateTime.now(clock);

        transactions.execute(() -> {
            orderRepository.findByIdForUpdate(deliveryOrderId)
                    .orElseThrow(() -> new NotFoundException("DELIVERY_ORDER_NOT_FOUND", "Delivery order not found: " + deliveryOrderId));

            DeliveryOrderRiderAssignment currentAssignment = riderRepository.findActiveAssignmentForOrder(deliveryOrderId, tenantId)
                    .orElseThrow(() -> new ConflictException("DELIVERY_RIDER_NO_ACTIVE_ASSIGNMENT", "No active rider assignment found to unassign"));

            UUID riderId = currentAssignment.getRiderId();
            currentAssignment.unassign(actor, now);
            riderRepository.saveAssignment(currentAssignment);

            if (eventPublisher != null) {
                eventPublisher.publishEvent(new DeliveryRiderEvents.DeliveryRiderUnassignedEvent(
                        tenantId, deliveryOrderId, riderId, now, actor
                ));
            }
            return null;
        });
    }

    @Override
    public List<DeliveryOrderRiderAssignment> getAssignmentHistory(UUID deliveryOrderId) {
        UUID tenantId = requireTenantId();
        return riderRepository.findAssignmentHistoryForOrder(deliveryOrderId, tenantId);
    }

    @Override
    public List<DeliveryRiderSummary> queryAvailableRiders(UUID zoneId, LocalDate date, UUID slotId) {
        UUID tenantId = requireTenantId();
        LocalDate targetDate = date != null ? date : LocalDate.now(clock);

        List<DeliveryRider> riders = riderRepository.findAll(tenantId, zoneId, DeliveryRiderStatus.ACTIVE, null);
        List<DeliveryRiderSummary> available = new ArrayList<>();

        for (DeliveryRider rider : riders) {
            int activeWorkload = riderRepository.countActiveAssignmentsForRider(rider.getId(), tenantId);
            if (activeWorkload >= rider.getMaxConcurrentDeliveries()) {
                continue; // Capacity reached
            }

            List<DeliveryRiderShift> shifts = riderRepository.findActiveShiftsByRiderIdAndDate(rider.getId(), targetDate, tenantId);
            Optional<DeliveryRiderShift> matchingShift = shifts.stream()
                    .filter(s -> slotId == null || slotId.equals(s.getDeliverySlotId()) || s.getDeliverySlotId() == null)
                    .findFirst();

            if (matchingShift.isEmpty() && !shifts.isEmpty()) {
                continue;
            }

            var driverSummary = driverEligibilityPort.findDriver(rider.getDriverId()).orElse(null);
            DeliveryRiderAvailability availability = computeAvailability(rider, activeWorkload, matchingShift);

            available.add(new DeliveryRiderSummary(
                    rider.getId(),
                    rider.getRiderCode(),
                    rider.getDriverId(),
                    driverSummary,
                    rider.getRiderType(),
                    rider.getStatus(),
                    availability,
                    rider.getPrimaryZoneId(),
                    rider.getSecondaryZoneIds(),
                    activeWorkload,
                    rider.getMaxConcurrentDeliveries(),
                    matchingShift
            ));
        }

        return available;
    }

    private void validateRiderEligibility(DeliveryRider rider, DeliveryOrder order, boolean isOverride, String overrideReason) {
        UUID tenantId = requireTenantId();

        if (rider.getStatus() != DeliveryRiderStatus.ACTIVE) {
            throw new ConflictException("DELIVERY_RIDER_INACTIVE", "Delivery rider is inactive or suspended: " + rider.getId());
        }

        // Validate driver eligibility
        var driver = driverEligibilityPort.findDriver(rider.getDriverId())
                .orElseThrow(() -> new NotFoundException("DRIVER_NOT_FOUND", "Driver not found"));
        if (!driver.active() || !"AVAILABLE".equalsIgnoreCase(driver.status())) {
            throw new ConflictException("DELIVERY_RIDER_DRIVER_NOT_ELIGIBLE", "Underlying driver is not active or available");
        }

        // Validate zone match
        UUID orderDestinationZoneId = zoneLookupPort.resolveZoneForLocation(order.destinationLocationId())
                .map(DeliveryZone::id)
                .orElse(null);

        if (orderDestinationZoneId != null && !rider.isEligibleForZone(orderDestinationZoneId)) {
            if (!isOverride) {
                throw new ConflictException("DELIVERY_RIDER_ZONE_MISMATCH", "Rider is not eligible for order destination zone: " + orderDestinationZoneId);
            }
            if (overrideReason == null || overrideReason.isBlank()) {
                throw new ConflictException("DELIVERY_RIDER_OVERRIDE_REASON_REQUIRED", "Override reason is mandatory for cross-zone assignment");
            }
        }

        // Validate workload capacity
        int activeWorkload = riderRepository.countActiveAssignmentsForRider(rider.getId(), tenantId);
        if (activeWorkload >= rider.getMaxConcurrentDeliveries()) {
            if (!isOverride) {
                throw new ConflictException("DELIVERY_RIDER_CAPACITY_EXCEEDED", "Rider has reached maximum concurrent delivery workload (" + rider.getMaxConcurrentDeliveries() + ")");
            }
            if (overrideReason == null || overrideReason.isBlank()) {
                throw new ConflictException("DELIVERY_RIDER_OVERRIDE_REASON_REQUIRED", "Override reason is mandatory when exceeding rider capacity");
            }
        }
    }

    private DeliveryRiderAvailability computeAvailability(DeliveryRider rider, int activeWorkload, Optional<DeliveryRiderShift> currentShift) {
        if (rider.getStatus() == DeliveryRiderStatus.INACTIVE || rider.getStatus() == DeliveryRiderStatus.SUSPENDED) {
            return DeliveryRiderAvailability.UNAVAILABLE;
        }
        if (currentShift.isEmpty()) {
            return DeliveryRiderAvailability.OFF_DUTY;
        }
        if (activeWorkload >= rider.getMaxConcurrentDeliveries()) {
            return DeliveryRiderAvailability.BUSY;
        }
        return DeliveryRiderAvailability.AVAILABLE;
    }

    private UUID requireTenantId() {
        return tenantContext.currentTenantId()
                .orElseThrow(() -> new IllegalStateException("Tenant context is missing"));
    }
}
