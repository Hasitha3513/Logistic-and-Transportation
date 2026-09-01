package com.transportlogistics.app.delivery.adapters.inbound.web.controllers;

import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.AssignRiderToDeliveryRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.CreateDeliveryRiderShiftRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.DeliveryRiderDutyStatusRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.OnboardDeliveryRiderRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.ReassignRiderToDeliveryRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.UpdateDeliveryRiderRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.response.DeliveryOrderRiderAssignmentResponse;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.response.DeliveryRiderResponse;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.response.DeliveryRiderShiftResponse;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.response.DeliveryRiderSummaryResponse;
import com.transportlogistics.app.delivery.domain.model.DeliveryOrderRiderAssignment;
import com.transportlogistics.app.delivery.domain.model.DeliveryRider;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderShift;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderType;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryRiderUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Delivery Riders", description = "Endpoints for managing last-mile delivery riders, shifts, and order assignments")
public class DeliveryRiderController {

    private final DeliveryRiderUseCase riderUseCase;

    public DeliveryRiderController(DeliveryRiderUseCase riderUseCase) {
        this.riderUseCase = riderUseCase;
    }

    @PostMapping("/delivery-riders")
    @PreAuthorize("hasAuthority('DELIVERY_RIDER_CREATE')")
    @Operation(summary = "Onboard a new delivery rider and link driver profile")
    public ResponseEntity<DeliveryRiderResponse> onboardRider(
            @Valid @RequestBody OnboardDeliveryRiderRequest request,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "system";
        DeliveryRider rider = riderUseCase.onboardRider(
                new DeliveryRiderUseCase.OnboardRiderCommand(
                        request.riderCode(),
                        request.driverId(),
                        request.riderType() != null ? request.riderType() : DeliveryRiderType.FULL_TIME,
                        request.transportMode(),
                        request.primaryZoneId(),
                        request.secondaryZoneIds(),
                        request.maxConcurrentDeliveries() != null ? request.maxConcurrentDeliveries() : 5
                ),
                actor
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(DeliveryRiderResponse.from(rider));
    }

    @GetMapping("/delivery-riders")
    @PreAuthorize("hasAuthority('DELIVERY_RIDER_VIEW')")
    @Operation(summary = "List delivery riders with zone and availability metrics")
    public ResponseEntity<List<DeliveryRiderSummaryResponse>> listRiders(
            @RequestParam(required = false) UUID zoneId,
            @RequestParam(required = false) DeliveryRiderStatus status,
            @RequestParam(required = false) DeliveryRiderType riderType
    ) {
        List<DeliveryRiderUseCase.DeliveryRiderSummary> riders = riderUseCase.listRiders(zoneId, status, riderType);
        return ResponseEntity.ok(riders.stream().map(DeliveryRiderSummaryResponse::from).toList());
    }

    @GetMapping("/delivery-riders/{id}")
    @PreAuthorize("hasAuthority('DELIVERY_RIDER_VIEW')")
    @Operation(summary = "Get delivery rider by ID")
    public ResponseEntity<DeliveryRiderResponse> getRider(@PathVariable UUID id) {
        DeliveryRider rider = riderUseCase.getRider(id);
        return ResponseEntity.ok(DeliveryRiderResponse.from(rider));
    }

    @PutMapping("/delivery-riders/{id}")
    @PreAuthorize("hasAuthority('DELIVERY_RIDER_UPDATE')")
    @Operation(summary = "Update delivery rider zones and capacity")
    public ResponseEntity<DeliveryRiderResponse> updateRider(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDeliveryRiderRequest request,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "system";
        DeliveryRider rider = riderUseCase.updateRider(
                id,
                new DeliveryRiderUseCase.UpdateRiderCommand(
                        request.primaryZoneId(),
                        request.transportMode(),
                        request.secondaryZoneIds(),
                        request.maxConcurrentDeliveries() != null ? request.maxConcurrentDeliveries() : 5
                ),
                actor
        );
        return ResponseEntity.ok(DeliveryRiderResponse.from(rider));
    }

    @PostMapping("/delivery-riders/{id}/activate")
    @PreAuthorize("hasAuthority('DELIVERY_RIDER_ACTIVATE')")
    @Operation(summary = "Activate a delivery rider")
    public ResponseEntity<DeliveryRiderResponse> activateRider(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "system";
        DeliveryRider rider = riderUseCase.activateRider(id, actor);
        return ResponseEntity.ok(DeliveryRiderResponse.from(rider));
    }

    @PostMapping("/delivery-riders/{id}/deactivate")
    @PreAuthorize("hasAuthority('DELIVERY_RIDER_ACTIVATE')")
    @Operation(summary = "Deactivate a delivery rider")
    public ResponseEntity<DeliveryRiderResponse> deactivateRider(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "system";
        DeliveryRider rider = riderUseCase.deactivateRider(id, actor);
        return ResponseEntity.ok(DeliveryRiderResponse.from(rider));
    }

    @PostMapping("/delivery-riders/{id}/suspend")
    @PreAuthorize("hasAuthority('DELIVERY_RIDER_ACTIVATE')")
    @Operation(summary = "Suspend a delivery rider")
    public ResponseEntity<DeliveryRiderResponse> suspendRider(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "system";
        DeliveryRider rider = riderUseCase.suspendRider(id, actor);
        return ResponseEntity.ok(DeliveryRiderResponse.from(rider));
    }

    // Shifts
    @PostMapping("/delivery-riders/{id}/shifts")
    @PreAuthorize("hasAuthority('DELIVERY_RIDER_UPDATE')")
    @Operation(summary = "Create a work shift for a rider")
    public ResponseEntity<DeliveryRiderShiftResponse> createShift(
            @PathVariable UUID id,
            @Valid @RequestBody CreateDeliveryRiderShiftRequest request,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "system";
        DeliveryRiderShift shift = riderUseCase.createShift(
                id,
                new DeliveryRiderUseCase.CreateShiftCommand(
                        request.shiftDate(),
                        request.startTime(),
                        request.endTime(),
                        request.deliverySlotId(),
                        request.maxDeliveries() != null ? request.maxDeliveries() : 5
                ),
                actor
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(DeliveryRiderShiftResponse.from(shift));
    }

    @GetMapping("/delivery-riders/{id}/shifts")
    @PreAuthorize("hasAuthority('DELIVERY_RIDER_VIEW')")
    @Operation(summary = "List shifts for a rider")
    public ResponseEntity<List<DeliveryRiderShiftResponse>> listShifts(@PathVariable UUID id) {
        List<DeliveryRiderShift> shifts = riderUseCase.listShifts(id);
        return ResponseEntity.ok(shifts.stream().map(DeliveryRiderShiftResponse::from).toList());
    }

    @PostMapping("/delivery-riders/{id}/duty-status")
    @PreAuthorize("hasAuthority('DELIVERY_RIDER_UPDATE')")
    @Operation(summary = "Update duty status of a shift (START_DUTY, COMPLETE_DUTY, CANCEL_SHIFT)")
    public ResponseEntity<DeliveryRiderShiftResponse> updateDutyStatus(
            @PathVariable UUID id,
            @Valid @RequestBody DeliveryRiderDutyStatusRequest request,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "system";
        DeliveryRiderShift shift = riderUseCase.updateDutyStatus(
                id,
                request.shiftId(),
                new DeliveryRiderUseCase.DutyStatusCommand(request.action()),
                actor
        );
        return ResponseEntity.ok(DeliveryRiderShiftResponse.from(shift));
    }

    @GetMapping("/delivery-riders/available")
    @PreAuthorize("hasAuthority('DELIVERY_RIDER_VIEW')")
    @Operation(summary = "Query available riders for zone/date/slot")
    public ResponseEntity<List<DeliveryRiderSummaryResponse>> queryAvailableRiders(
            @RequestParam(required = false) UUID zoneId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID slotId
    ) {
        List<DeliveryRiderUseCase.DeliveryRiderSummary> available = riderUseCase.queryAvailableRiders(zoneId, date, slotId);
        return ResponseEntity.ok(available.stream().map(DeliveryRiderSummaryResponse::from).toList());
    }

    // Delivery Order Assignment Commands
    @PostMapping("/deliveries/{id}/assign-rider")
    @PreAuthorize("hasAuthority('DELIVERY_RIDER_ASSIGN')")
    @Operation(summary = "Assign a rider to a delivery order")
    public ResponseEntity<DeliveryOrderRiderAssignmentResponse> assignRider(
            @PathVariable UUID id,
            @Valid @RequestBody AssignRiderToDeliveryRequest request,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "system";
        DeliveryOrderRiderAssignment assignment = riderUseCase.assignRider(
                id,
                new DeliveryRiderUseCase.AssignRiderCommand(
                        request.riderId(),
                        request.isOverride(),
                        request.overrideReason()
                ),
                actor
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(DeliveryOrderRiderAssignmentResponse.from(assignment));
    }

    @PostMapping("/deliveries/{id}/reassign-rider")
    @PreAuthorize("hasAuthority('DELIVERY_RIDER_ASSIGN')")
    @Operation(summary = "Reassign a delivery order to another rider")
    public ResponseEntity<DeliveryOrderRiderAssignmentResponse> reassignRider(
            @PathVariable UUID id,
            @Valid @RequestBody ReassignRiderToDeliveryRequest request,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "system";
        DeliveryOrderRiderAssignment assignment = riderUseCase.reassignRider(
                id,
                new DeliveryRiderUseCase.ReassignRiderCommand(
                        request.newRiderId(),
                        request.isOverride(),
                        request.overrideReason()
                ),
                actor
        );
        return ResponseEntity.ok(DeliveryOrderRiderAssignmentResponse.from(assignment));
    }

    @PostMapping("/deliveries/{id}/unassign-rider")
    @PreAuthorize("hasAuthority('DELIVERY_RIDER_ASSIGN')")
    @Operation(summary = "Unassign rider from delivery order")
    public ResponseEntity<Void> unassignRider(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "system";
        riderUseCase.unassignRider(id, actor);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/deliveries/{id}/rider-history")
    @PreAuthorize("hasAuthority('DELIVERY_RIDER_VIEW')")
    @Operation(summary = "Get rider assignment history for a delivery order")
    public ResponseEntity<List<DeliveryOrderRiderAssignmentResponse>> getRiderHistory(@PathVariable UUID id) {
        List<DeliveryOrderRiderAssignment> history = riderUseCase.getAssignmentHistory(id);
        return ResponseEntity.ok(history.stream().map(DeliveryOrderRiderAssignmentResponse::from).toList());
    }
}
