package com.transportlogistics.app.delivery.adapters.inbound.web.controllers;

import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.CreateDeliveryZoneRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.UpdateDeliveryZoneRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.response.DeliveryZoneResponse;
import com.transportlogistics.app.delivery.domain.model.DeliveryZone;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneStatus;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryZoneUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/delivery-zones")
public class DeliveryZoneController {

    private final DeliveryZoneUseCase zoneUseCase;

    public DeliveryZoneController(DeliveryZoneUseCase zoneUseCase) {
        this.zoneUseCase = zoneUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('DELIVERY_ZONE_CREATE')")
    public ResponseEntity<DeliveryZoneResponse> createZone(
            @Valid @RequestBody CreateDeliveryZoneRequest request,
            java.security.Principal principal
    ) {
        DeliveryZoneUseCase.CreateZoneCommand command = new DeliveryZoneUseCase.CreateZoneCommand(
                request.zoneCode(),
                request.zoneName(),
                request.description(),
                request.zoneType(),
                request.serviceable() == null || request.serviceable(),
                request.dailyCapacity(),
                request.depotLocationId(),
                request.coordinates(),
                request.priority() != null ? request.priority() : 0
        );
        String actor = principal != null ? principal.getName() : "system";
        DeliveryZone created = zoneUseCase.createZone(command, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(DeliveryZoneResponse.fromDomain(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DELIVERY_ZONE_UPDATE')")
    public ResponseEntity<DeliveryZoneResponse> updateZone(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDeliveryZoneRequest request,
            java.security.Principal principal
    ) {
        DeliveryZoneUseCase.UpdateZoneCommand command = new DeliveryZoneUseCase.UpdateZoneCommand(
                request.zoneName(),
                request.description(),
                request.zoneType(),
                request.serviceable() == null || request.serviceable(),
                request.dailyCapacity(),
                request.depotLocationId(),
                request.coordinates(),
                request.priority() != null ? request.priority() : 0,
                request.expectedVersion()
        );
        String actor = principal != null ? principal.getName() : "system";
        DeliveryZone updated = zoneUseCase.updateZone(id, command, actor);
        return ResponseEntity.ok(DeliveryZoneResponse.fromDomain(updated));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('DELIVERY_ZONE_ACTIVATE')")
    public ResponseEntity<DeliveryZoneResponse> activateZone(
            @PathVariable UUID id,
            java.security.Principal principal
    ) {
        String actor = principal != null ? principal.getName() : "system";
        DeliveryZone activated = zoneUseCase.activateZone(id, actor);
        return ResponseEntity.ok(DeliveryZoneResponse.fromDomain(activated));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('DELIVERY_ZONE_ACTIVATE')")
    public ResponseEntity<DeliveryZoneResponse> deactivateZone(
            @PathVariable UUID id,
            java.security.Principal principal
    ) {
        String actor = principal != null ? principal.getName() : "system";
        DeliveryZone deactivated = zoneUseCase.deactivateZone(id, actor);
        return ResponseEntity.ok(DeliveryZoneResponse.fromDomain(deactivated));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DELIVERY_ZONE_VIEW')")
    public ResponseEntity<DeliveryZoneResponse> getZone(@PathVariable UUID id) {
        DeliveryZone zone = zoneUseCase.getZone(id);
        return ResponseEntity.ok(DeliveryZoneResponse.fromDomain(zone));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DELIVERY_ZONE_VIEW')")
    public ResponseEntity<List<DeliveryZoneResponse>> listZones(
            @RequestParam(required = false) DeliveryZoneStatus status,
            @RequestParam(required = false) Boolean serviceable
    ) {
        List<DeliveryZoneResponse> list = zoneUseCase.listZones(status, serviceable)
                .stream()
                .map(DeliveryZoneResponse::fromDomain)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/resolve")
    @PreAuthorize("hasAuthority('DELIVERY_ZONE_VIEW')")
    public ResponseEntity<DeliveryZoneResponse> resolveZone(
            @RequestParam(required = false) UUID locationId,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double latitude
    ) {
        if (locationId != null) {
            return zoneUseCase.resolveZoneForLocation(locationId)
                    .map(z -> ResponseEntity.ok(DeliveryZoneResponse.fromDomain(z)))
                    .orElse(ResponseEntity.noContent().build());
        }
        if (longitude != null && latitude != null) {
            return zoneUseCase.resolveZoneForCoordinates(longitude, latitude)
                    .map(z -> ResponseEntity.ok(DeliveryZoneResponse.fromDomain(z)))
                    .orElse(ResponseEntity.noContent().build());
        }
        return ResponseEntity.badRequest().build();
    }
}
