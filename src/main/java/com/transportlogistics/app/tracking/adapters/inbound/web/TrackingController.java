package com.transportlogistics.app.tracking.adapters.inbound.web.controllers;

import com.transportlogistics.app.tenancy.CurrentTenant;
import com.transportlogistics.app.tracking.domain.TrackingModels.Association;
import com.transportlogistics.app.tracking.domain.TrackingModels.Device;
import com.transportlogistics.app.tracking.domain.TrackingModels.DeviceLifecycle;
import com.transportlogistics.app.tracking.domain.TrackingModels.State;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase.Associate;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase.Context;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase.CreateDevice;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase.HistoryPage;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase.UpdateDevice;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/v1/tracking")
public class TrackingController {
 private final TrackingUseCase use; private final CurrentTenant tenants;
 public TrackingController(TrackingUseCase use,CurrentTenant tenants){this.use=use;this.tenants=tenants;}
 @GetMapping("/vehicles") @PreAuthorize("hasAuthority('TRACKING_VIEW')") public List<State> vehicles(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return use.vehicles(tenant(),page,size,Instant.now());}
 @GetMapping("/vehicles/{id}/latest") @PreAuthorize("hasAuthority('TRACKING_VIEW')") public State latest(@PathVariable UUID id){return use.latest(tenant(),id,Instant.now());}
 @GetMapping("/vehicles/{id}/positions") @PreAuthorize("hasAuthority('TRACKING_HISTORY_VIEW')") public HistoryPage history(@PathVariable UUID id,@RequestParam Instant from,@RequestParam Instant to,@RequestParam(required=false)String cursor,@RequestParam(defaultValue="100")int limit){return use.positions(tenant(),id,from,to,cursor,limit);}
 @GetMapping("/devices") @PreAuthorize("hasAuthority('TRACKING_VIEW')") public List<Device> devices(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size,Authentication a){return use.devices(tenant(),page,size,manage(a));}
 @GetMapping("/devices/{id}") @PreAuthorize("hasAuthority('TRACKING_VIEW')") public Device device(@PathVariable UUID id,Authentication a){return use.get(tenant(),id,manage(a));}
 @PostMapping("/devices") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('TRACKING_DEVICE_MANAGE')") public Device create(@Valid @RequestBody DeviceRequest r){return use.create(context(),new CreateDevice(r.externalDeviceReference(),r.providerAlias(),r.hardwareSerialReference()));}
 @PutMapping("/devices/{id}") @PreAuthorize("hasAuthority('TRACKING_DEVICE_MANAGE')") public Device update(@PathVariable UUID id,@Valid @RequestBody DeviceUpdate r){return use.update(context(),id,r.version(),new UpdateDevice(r.externalDeviceReference(),r.hardwareSerialReference()));}
 @PostMapping("/devices/{id}/activate") @PreAuthorize("hasAuthority('TRACKING_DEVICE_MANAGE')") public Device activate(@PathVariable UUID id,@Valid @RequestBody VersionRequest r){return use.lifecycle(context(),id,r.version(),DeviceLifecycle.ACTIVE);}
 @PostMapping("/devices/{id}/disable") @PreAuthorize("hasAuthority('TRACKING_DEVICE_MANAGE')") public Device disable(@PathVariable UUID id,@Valid @RequestBody VersionRequest r){return use.lifecycle(context(),id,r.version(),DeviceLifecycle.DISABLED);}
 @PostMapping("/devices/{id}/associations") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('TRACKING_DEVICE_MANAGE')") public Association associate(@PathVariable UUID id,@Valid @RequestBody AssociationRequest r){return use.associate(context(),id,new Associate(r.vehicleId(),r.effectiveFrom()));}
 @PostMapping("/devices/{id}/associations/{associationId}/end") @PreAuthorize("hasAuthority('TRACKING_DEVICE_MANAGE')") public Association end(@PathVariable UUID id,@PathVariable UUID associationId,@Valid @RequestBody EndAssociationRequest r){return use.endAssociation(context(),id,associationId,r.effectiveTo());}
 private UUID tenant(){return tenants.required().tenantId();} private Context context(){var c=tenants.required();return new Context(c.tenantId(),c.actorId(),c.correlationId());}
 private static boolean manage(Authentication a){return a.getAuthorities().stream().anyMatch(x->x.getAuthority().equals("TRACKING_DEVICE_MANAGE"));}
 public record DeviceRequest(@NotBlank @Size(max=160)String externalDeviceReference,@NotBlank @Size(max=80)String providerAlias,@Size(max=160)String hardwareSerialReference){}
 public record DeviceUpdate(@NotBlank @Size(max=160)String externalDeviceReference,@Size(max=160)String hardwareSerialReference,@PositiveOrZero long version){}
 public record VersionRequest(@PositiveOrZero long version){}
 public record AssociationRequest(@NotNull UUID vehicleId,@NotNull Instant effectiveFrom){}
 public record EndAssociationRequest(@NotNull Instant effectiveTo){}
}
