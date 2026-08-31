package com.transportlogistics.app.delivery.application;

import com.transportlogistics.app.delivery.domain.model.*;
import com.transportlogistics.app.delivery.ports.inbound.DeliverySlotUseCase;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryZoneLookupPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryLocationLookupPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryOrderRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryOrderTransaction;
import com.transportlogistics.app.delivery.ports.outbound.DeliverySlotAvailabilityPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliverySlotRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class DeliverySlotService implements DeliverySlotUseCase, DeliverySlotAvailabilityPort {
    private final DeliverySlotRepository slotRepository;
    private final DeliveryZoneLookupPort zoneLookupPort;
    private final DeliveryOrderRepository orderRepository;
    private final DeliveryTenantContextPort tenantContext;
    private final DeliveryOrderTransaction transactions;
    private final Clock clock;

    public DeliverySlotService(
            DeliverySlotRepository slotRepository,
            DeliveryZoneLookupPort zoneLookupPort,
            DeliveryOrderRepository orderRepository,
            DeliveryLocationLookupPort locationLookupPort,
            DeliveryTenantContextPort tenantContext,
            DeliveryOrderTransaction transactions,
            Clock clock
    ) {
        this.slotRepository = slotRepository;
        this.zoneLookupPort = zoneLookupPort;
        this.orderRepository = orderRepository;
        this.tenantContext = tenantContext;
        this.transactions = transactions;
        this.clock = clock;
    }

    private UUID currentTenantId() {
        return tenantContext.currentTenantId()
                .orElseThrow(() -> new BusinessRuleException("TENANT_CONTEXT_REQUIRED", "Tenant context is required"));
    }

    @Override
    public DeliverySlot createSlot(CreateSlotCommand command, String actor) {
        UUID tenantId = currentTenantId();
        OffsetDateTime now = OffsetDateTime.now(clock);

        return transactions.execute(() -> {
            // Lock zone to serialize slot creation per zone and avoid race conditions
            DeliveryZone zone = zoneLookupPort.findZoneForUpdate(command.deliveryZoneId())
                    .orElseThrow(() -> new NotFoundException("DELIVERY_ZONE_NOT_FOUND", "Delivery zone not found: " + command.deliveryZoneId()));
            if (zone.status() != DeliveryZoneStatus.ACTIVE) {
                throw new ConflictException("DELIVERY_ZONE_INACTIVE", "Cannot create slot for inactive delivery zone");
            }

            DeliverySlot slot = DeliverySlot.create(
                    UUID.randomUUID(),
                    tenantId,
                    command.deliveryZoneId(),
                    command.slotDate(),
                    command.startTime(),
                    command.endTime(),
                    command.slotType(),
                    command.maxCapacity(),
                    command.cutoffTime(),
                    command.bufferMinutes(),
                    actor != null ? actor : "system",
                    now
            );

            if (slotRepository.existsOverlapping(slot)) {
                throw new ConflictException("DELIVERY_SLOT_OVERLAP", "An overlapping active slot already exists for this zone, date, and slot type");
            }

            return slotRepository.save(slot);
        });
    }

    @Override
    public DeliverySlot getSlot(UUID id) {
        return slotRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("DELIVERY_SLOT_NOT_FOUND", "Delivery slot not found: " + id));
    }

    @Override
    public List<DeliverySlot> listSlots(UUID zoneId, LocalDate startDate, LocalDate endDate) {
        if (zoneId != null && startDate != null && endDate != null) {
            return slotRepository.findByZoneAndDateRange(zoneId, startDate, endDate);
        } else if (zoneId != null && startDate != null) {
            return slotRepository.findByZoneAndDate(zoneId, startDate);
        } else if (startDate != null) {
            return slotRepository.findByDate(startDate);
        } else {
            return slotRepository.findByDate(LocalDate.now(clock));
        }
    }

    @Override
    public List<DeliverySlot> getAvailableSlots(UUID deliveryZoneId, LocalDate date) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<DeliverySlot> slots = slotRepository.findByZoneAndDate(deliveryZoneId, date);
        return slots.stream()
                .filter(slot -> slot.isAvailableForBooking(now, false))
                .collect(Collectors.toList());
    }

    @Override
    public DeliverySlot updateSlot(UUID id, UpdateSlotCommand command, String actor) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        return transactions.execute(() -> {
            DeliverySlot slot = getSlot(id);
            DeliverySlot updated = slot.update(
                    command.startTime(),
                    command.endTime(),
                    command.slotType(),
                    command.maxCapacity(),
                    command.cutoffTime(),
                    command.bufferMinutes(),
                    command.expectedVersion(),
                    actor != null ? actor : "system",
                    now
            );

            if ((!slot.getStartTime().equals(command.startTime()) || !slot.getEndTime().equals(command.endTime()))
                    && slotRepository.existsOverlapping(updated)) {
                throw new ConflictException("DELIVERY_SLOT_OVERLAP", "An overlapping active slot already exists");
            }

            return slotRepository.save(updated);
        });
    }

    @Override
    public DeliverySlot activateSlot(UUID id, long expectedVersion, String actor) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        return transactions.execute(() -> {
            DeliverySlot slot = getSlot(id);
            DeliverySlot activated = slot.activate(expectedVersion, actor != null ? actor : "system", now);
            if (slotRepository.existsOverlapping(activated)) {
                throw new ConflictException("DELIVERY_SLOT_OVERLAP", "An overlapping active slot already exists");
            }
            return slotRepository.save(activated);
        });
    }

    @Override
    public DeliverySlot deactivateSlot(UUID id, long expectedVersion, String actor) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        return transactions.execute(() -> {
            DeliverySlot slot = getSlot(id);
            return slotRepository.save(slot.deactivate(expectedVersion, actor != null ? actor : "system", now));
        });
    }

    @Override
    public DeliverySlot closeSlot(UUID id, long expectedVersion, String actor) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        return transactions.execute(() -> {
            DeliverySlot slot = getSlot(id);
            return slotRepository.save(slot.close(expectedVersion, actor != null ? actor : "system", now));
        });
    }

    @Override
    public DeliverySlotReservation assignDeliveryOrder(UUID slotId, AssignSlotCommand command, String actor) {
        UUID tenantId = currentTenantId();
        OffsetDateTime now = OffsetDateTime.now(clock);
        String currentActor = actor != null ? actor : "system";

        return transactions.execute(() -> {
            DeliverySlot slot = slotRepository.findByIdForUpdate(slotId)
                    .orElseThrow(() -> new NotFoundException("DELIVERY_SLOT_NOT_FOUND", "Delivery slot not found: " + slotId));
            DeliveryOrder order = orderRepository.findById(command.deliveryOrderId())
                    .orElseThrow(() -> new NotFoundException("DELIVERY_ORDER_NOT_FOUND", "Delivery order not found: " + command.deliveryOrderId()));

            if (order.status() == DeliveryStatus.DELIVERED || order.status() == DeliveryStatus.RETURN_TO_BASE) {
                throw new ConflictException("DELIVERY_ORDER_TERMINAL", "Cannot assign terminal delivery order to a slot");
            }

            // Verify order destination zone matches slot's zone
            UUID destinationZoneId = zoneLookupPort.resolveZoneForLocation(order.destinationLocationId())
                    .map(DeliveryZone::id)
                    .orElse(null);

            if (destinationZoneId != null && !destinationZoneId.equals(slot.getDeliveryZoneId())) {
                throw new ConflictException("DELIVERY_SLOT_ZONE_MISMATCH", "Order destination zone does not match delivery slot zone");
            }

            // Check if zone is serviceable and lock zone to enforce zone daily capacity atomically
            DeliveryZone zone = zoneLookupPort.findZoneForUpdate(slot.getDeliveryZoneId())
                    .orElseThrow(() -> new NotFoundException("DELIVERY_ZONE_NOT_FOUND", "Delivery zone not found"));
            if (zone.status() != DeliveryZoneStatus.ACTIVE) {
                throw new ConflictException("DELIVERY_ZONE_INACTIVE", "Delivery zone is inactive");
            }
            if (!zone.serviceable()) {
                throw new ConflictException("DELIVERY_ZONE_NOT_SERVICEABLE", "Delivery zone is temporarily non-serviceable");
            }

            // Check zone daily capacity if configured
            if (zone.dailyCapacity() != null && zone.dailyCapacity() > 0 && !command.isOverride()) {
                int totalBookingsInZone = slotRepository.countActiveBookingsInZoneOnDate(slot.getDeliveryZoneId(), slot.getSlotDate());
                if (totalBookingsInZone >= zone.dailyCapacity()) {
                    throw new ConflictException("DELIVERY_ZONE_DAILY_CAPACITY_EXCEEDED", "Delivery zone daily capacity ceiling reached (" + zone.dailyCapacity() + ")");
                }
            }

            // Check if order already has an active reservation
            Optional<DeliverySlotReservation> existingReservation = slotRepository.findActiveReservationForOrder(order.id().value());
            if (existingReservation.isPresent()) {
                if (existingReservation.get().getDeliverySlotId().equals(slotId)) {
                    return existingReservation.get();
                } else {
                    throw new ConflictException("DELIVERY_SLOT_ASSIGNMENT_EXISTS", "Delivery order already has an active reservation on slot " + existingReservation.get().getDeliverySlotId());
                }
            }

            // Reserve slot
            DeliverySlot reservedSlot = slot.reserve(command.isOverride(), command.overrideReason(), currentActor, now);
            slotRepository.save(reservedSlot);

            // Create reservation record
            DeliverySlotReservation reservation = DeliverySlotReservation.create(
                    UUID.randomUUID(),
                    tenantId,
                    slotId,
                    order.id().value(),
                    now,
                    currentActor,
                    command.isOverride(),
                    command.overrideReason()
            );
            return slotRepository.saveReservation(reservation);
        });
    }

    @Override
    public DeliverySlotReservation releaseReservation(UUID slotId, UUID deliveryOrderId, String actor) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        String currentActor = actor != null ? actor : "system";

        return transactions.execute(() -> {
            DeliverySlot slot = getSlot(slotId);
            DeliverySlotReservation reservation = slotRepository.findActiveReservationForOrder(deliveryOrderId)
                    .filter(res -> res.getDeliverySlotId().equals(slotId))
                    .orElseThrow(() -> new NotFoundException("DELIVERY_SLOT_RESERVATION_NOT_FOUND", "Active reservation not found for order: " + deliveryOrderId));

            DeliverySlotReservation released = reservation.release(now, currentActor);
            slotRepository.saveReservation(released);

            DeliverySlot releasedSlot = slot.release(currentActor, now);
            slotRepository.save(releasedSlot);

            return released;
        });
    }

    @Override
    public DeliverySlotReservation reassignDeliveryOrder(UUID newSlotId, UUID deliveryOrderId, boolean isOverride, String overrideReason, String actor) {
        return transactions.execute(() -> {
            Optional<DeliverySlotReservation> activeOpt = slotRepository.findActiveReservationForOrder(deliveryOrderId);
            if (activeOpt.isPresent()) {
                DeliverySlotReservation currentReservation = activeOpt.get();
                if (currentReservation.getDeliverySlotId().equals(newSlotId)) {
                    return currentReservation;
                }
                releaseReservation(currentReservation.getDeliverySlotId(), deliveryOrderId, actor);
            }
            return assignDeliveryOrder(newSlotId, new AssignSlotCommand(deliveryOrderId, isOverride, overrideReason), actor);
        });
    }

    @Override
    public List<DeliverySlotReservation> listReservations(UUID slotId) {
        return slotRepository.findReservationsBySlotId(slotId);
    }

    @Override
    public SlotAvailability checkAvailability(UUID destinationLocationId, OffsetDateTime requestedFrom, OffsetDateTime requestedTo) {
        if (destinationLocationId == null || requestedFrom == null || requestedTo == null) {
            return new SlotAvailability(false, "MISSING_PARAMETERS");
        }

        return zoneLookupPort.resolveZoneForLocation(destinationLocationId)
                .map(zone -> {
                    if (zone.status() != DeliveryZoneStatus.ACTIVE || !zone.serviceable()) {
                        return new SlotAvailability(false, "ZONE_UNAVAILABLE");
                    }
                    LocalDate date = requestedFrom.toLocalDate();
                    OffsetDateTime now = OffsetDateTime.now(clock);
                    List<DeliverySlot> slots = slotRepository.findByZoneAndDate(zone.id(), date);
                    boolean hasAvailable = slots.stream().anyMatch(s -> s.isAvailableForBooking(now, false));
                    return new SlotAvailability(hasAvailable, hasAvailable ? "SLOT_AVAILABLE" : "CAPACITY_EXHAUSTED");
                })
                .orElse(new SlotAvailability(true, "NO_ZONE_DEFAULT"));
    }
}
