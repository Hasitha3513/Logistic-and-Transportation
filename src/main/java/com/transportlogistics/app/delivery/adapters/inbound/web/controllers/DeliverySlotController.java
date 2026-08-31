package com.transportlogistics.app.delivery.adapters.inbound.web.controllers;

import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.AssignDeliverySlotRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.CreateDeliverySlotRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.UpdateDeliverySlotRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.response.DeliverySlotReservationResponse;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.response.DeliverySlotResponse;
import com.transportlogistics.app.delivery.domain.model.DeliverySlot;
import com.transportlogistics.app.delivery.domain.model.DeliverySlotReservation;
import com.transportlogistics.app.delivery.ports.inbound.DeliverySlotUseCase;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/delivery-slots")
public class DeliverySlotController {
    private final DeliverySlotUseCase slotUseCase;

    public DeliverySlotController(DeliverySlotUseCase slotUseCase) {
        this.slotUseCase = slotUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('DELIVERY_SLOT_CREATE')")
    public ResponseEntity<DeliverySlotResponse> createSlot(
            @Valid @RequestBody CreateDeliverySlotRequest request,
            Principal principal
    ) {
        DeliverySlotUseCase.CreateSlotCommand command = new DeliverySlotUseCase.CreateSlotCommand(
                request.deliveryZoneId(),
                request.slotDate(),
                request.startTime(),
                request.endTime(),
                request.slotType(),
                request.maxCapacity(),
                request.cutoffTime(),
                request.bufferMinutes()
        );
        String actor = principal != null ? principal.getName() : "system";
        DeliverySlot slot = slotUseCase.createSlot(command, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(DeliverySlotResponse.fromDomain(slot));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DELIVERY_SLOT_VIEW')")
    public ResponseEntity<DeliverySlotResponse> getSlot(@PathVariable UUID id) {
        DeliverySlot slot = slotUseCase.getSlot(id);
        return ResponseEntity.ok(DeliverySlotResponse.fromDomain(slot));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DELIVERY_SLOT_VIEW')")
    public ResponseEntity<List<DeliverySlotResponse>> listSlots(
            @RequestParam(required = false) UUID zoneId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<DeliverySlot> slots = slotUseCase.listSlots(zoneId, startDate, endDate);
        return ResponseEntity.ok(slots.stream().map(DeliverySlotResponse::fromDomain).collect(Collectors.toList()));
    }

    @GetMapping("/available")
    @PreAuthorize("hasAuthority('DELIVERY_SLOT_VIEW')")
    public ResponseEntity<List<DeliverySlotResponse>> getAvailableSlots(
            @RequestParam UUID zoneId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<DeliverySlot> slots = slotUseCase.getAvailableSlots(zoneId, date);
        return ResponseEntity.ok(slots.stream().map(DeliverySlotResponse::fromDomain).collect(Collectors.toList()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DELIVERY_SLOT_UPDATE')")
    public ResponseEntity<DeliverySlotResponse> updateSlot(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDeliverySlotRequest request,
            Principal principal
    ) {
        DeliverySlotUseCase.UpdateSlotCommand command = new DeliverySlotUseCase.UpdateSlotCommand(
                request.startTime(),
                request.endTime(),
                request.slotType(),
                request.maxCapacity(),
                request.cutoffTime(),
                request.bufferMinutes(),
                request.expectedVersion()
        );
        String actor = principal != null ? principal.getName() : "system";
        DeliverySlot updated = slotUseCase.updateSlot(id, command, actor);
        return ResponseEntity.ok(DeliverySlotResponse.fromDomain(updated));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('DELIVERY_SLOT_ACTIVATE')")
    public ResponseEntity<DeliverySlotResponse> activateSlot(
            @PathVariable UUID id,
            @RequestParam long expectedVersion,
            Principal principal
    ) {
        String actor = principal != null ? principal.getName() : "system";
        DeliverySlot activated = slotUseCase.activateSlot(id, expectedVersion, actor);
        return ResponseEntity.ok(DeliverySlotResponse.fromDomain(activated));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('DELIVERY_SLOT_ACTIVATE')")
    public ResponseEntity<DeliverySlotResponse> deactivateSlot(
            @PathVariable UUID id,
            @RequestParam long expectedVersion,
            Principal principal
    ) {
        String actor = principal != null ? principal.getName() : "system";
        DeliverySlot deactivated = slotUseCase.deactivateSlot(id, expectedVersion, actor);
        return ResponseEntity.ok(DeliverySlotResponse.fromDomain(deactivated));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('DELIVERY_SLOT_ACTIVATE')")
    public ResponseEntity<DeliverySlotResponse> closeSlot(
            @PathVariable UUID id,
            @RequestParam long expectedVersion,
            Principal principal
    ) {
        String actor = principal != null ? principal.getName() : "system";
        DeliverySlot closed = slotUseCase.closeSlot(id, expectedVersion, actor);
        return ResponseEntity.ok(DeliverySlotResponse.fromDomain(closed));
    }

    @PostMapping("/{id}/reservations")
    @PreAuthorize("hasAuthority('DELIVERY_SLOT_ASSIGN') or (hasAuthority('DELIVERY_SLOT_OVERRIDE') and #request.override)")
    public ResponseEntity<DeliverySlotReservationResponse> assignDelivery(
            @PathVariable UUID id,
            @Valid @RequestBody AssignDeliverySlotRequest request,
            Principal principal
    ) {
        DeliverySlotUseCase.AssignSlotCommand command = new DeliverySlotUseCase.AssignSlotCommand(
                request.deliveryOrderId(),
                request.isOverride(),
                request.overrideReason()
        );
        String actor = principal != null ? principal.getName() : "system";
        DeliverySlotReservation reservation = slotUseCase.assignDeliveryOrder(id, command, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(DeliverySlotReservationResponse.fromDomain(reservation));
    }

    @PostMapping("/{id}/reservations/{deliveryOrderId}/release")
    @PreAuthorize("hasAuthority('DELIVERY_SLOT_ASSIGN')")
    public ResponseEntity<DeliverySlotReservationResponse> releaseReservation(
            @PathVariable UUID id,
            @PathVariable UUID deliveryOrderId,
            Principal principal
    ) {
        String actor = principal != null ? principal.getName() : "system";
        DeliverySlotReservation released = slotUseCase.releaseReservation(id, deliveryOrderId, actor);
        return ResponseEntity.ok(DeliverySlotReservationResponse.fromDomain(released));
    }

    @GetMapping("/{id}/reservations")
    @PreAuthorize("hasAuthority('DELIVERY_SLOT_VIEW')")
    public ResponseEntity<List<DeliverySlotReservationResponse>> listReservations(@PathVariable UUID id) {
        List<DeliverySlotReservation> reservations = slotUseCase.listReservations(id);
        return ResponseEntity.ok(reservations.stream().map(DeliverySlotReservationResponse::fromDomain).collect(Collectors.toList()));
    }
}
