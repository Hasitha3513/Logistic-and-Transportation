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
import com.transportlogistics.app.tracking.ports.inbound.TrackingProviderManagementUseCase;
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
 private final TrackingUseCase use; private final TrackingProviderManagementUseCase providers; private final CurrentTenant tenants;
 public TrackingController(TrackingUseCase use,TrackingProviderManagementUseCase providers,CurrentTenant tenants){this.use=use;this.providers=providers;this.tenants=tenants;}
 @GetMapping("/vehicles") @PreAuthorize("hasAuthority('TRACKING_VIEW')") public List<State> vehicles(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return use.vehicles(tenant(),page,size,Instant.now());}
 @GetMapping("/vehicles/{id}/latest") @PreAuthorize("hasAuthority('TRACKING_VIEW')") public State latest(@PathVariable UUID id){return use.latest(tenant(),id,Instant.now());}
 @GetMapping("/vehicles/{id}/positions") @PreAuthorize("hasAuthority('TRACKING_HISTORY_VIEW')") public HistoryPage history(@PathVariable UUID id,@RequestParam Instant from,@RequestParam Instant to,@RequestParam(required=false)String cursor,@RequestParam(defaultValue="100")int limit){return use.positions(tenant(),id,from,to,cursor,limit);}
 @GetMapping("/devices") @PreAuthorize("hasAuthority('TRACKING_VIEW')") public List<DeviceResponse> devices(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size,Authentication a){boolean reveal=manage(a);return use.devices(tenant(),page,size,reveal).stream().map(d->response(d,reveal)).toList();}
 @GetMapping("/devices/{id}") @PreAuthorize("hasAuthority('TRACKING_VIEW')") public DeviceResponse device(@PathVariable UUID id,Authentication a){boolean reveal=manage(a);return response(use.get(tenant(),id,reveal),reveal);}
 @PostMapping("/devices") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('TRACKING_DEVICE_MANAGE')") public DeviceResponse create(@Valid @RequestBody DeviceRequest r){return response(use.create(context(),new CreateDevice(r.externalDeviceReference(),r.providerAlias(),r.hardwareSerialReference())),true);}
 @PutMapping("/devices/{id}") @PreAuthorize("hasAuthority('TRACKING_DEVICE_MANAGE')") public DeviceResponse update(@PathVariable UUID id,@Valid @RequestBody DeviceUpdate r){return response(use.update(context(),id,r.version(),new UpdateDevice(r.externalDeviceReference(),r.hardwareSerialReference())),true);}
 @PostMapping("/devices/{id}/activate") @PreAuthorize("hasAuthority('TRACKING_DEVICE_MANAGE')") public DeviceResponse activate(@PathVariable UUID id,@Valid @RequestBody VersionRequest r){return response(use.lifecycle(context(),id,r.version(),DeviceLifecycle.ACTIVE),true);}
 @PostMapping("/devices/{id}/disable") @PreAuthorize("hasAuthority('TRACKING_DEVICE_MANAGE')") public DeviceResponse disable(@PathVariable UUID id,@Valid @RequestBody VersionRequest r){return response(use.lifecycle(context(),id,r.version(),DeviceLifecycle.DISABLED),true);}
 @PostMapping("/devices/{id}/retire") @PreAuthorize("hasAuthority('TRACKING_DEVICE_MANAGE')") public DeviceResponse retire(@PathVariable UUID id,@Valid @RequestBody VersionRequest r){return response(use.lifecycle(context(),id,r.version(),DeviceLifecycle.RETIRED),true);}
 @PostMapping("/devices/{id}/associations") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('TRACKING_DEVICE_MANAGE')") public Association associate(@PathVariable UUID id,@Valid @RequestBody AssociationRequest r){return use.associate(context(),id,new Associate(r.vehicleId(),r.effectiveFrom()));}
 @PostMapping("/devices/{id}/associations/{associationId}/end") @PreAuthorize("hasAuthority('TRACKING_DEVICE_MANAGE')") public Association end(@PathVariable UUID id,@PathVariable UUID associationId,@Valid @RequestBody EndAssociationRequest r){return use.endAssociation(context(),id,associationId,r.effectiveTo());}
 private UUID tenant(){return tenants.required().tenantId();} private Context context(){var c=tenants.required();return new Context(c.tenantId(),c.actorId(),c.correlationId());}
 private static boolean manage(Authentication a){return a.getAuthorities().stream().anyMatch(x->x.getAuthority().equals("TRACKING_DEVICE_MANAGE"));}
 private DeviceResponse response(Device d,boolean reveal){CurrentProviderBinding binding=null;if(reveal){var current=providers.currentBinding(d.tenantId(),d.id());if(current.isPresent()){var b=current.orElseThrow();var connection=providers.get(d.tenantId(),b.providerConnectionId());binding=new CurrentProviderBinding(b.id(),b.providerConnectionId().value(),b.lifecycle().name(),b.version(),connection.displayName(),connection.providerType().value(),connection.providerAlias(),connection.lifecycle().name(),mask(b.externalDeviceReference()),b.safeConfiguration().values());}}var association=use.activeAssociation(d.tenantId(),d.id()).map(a->new CurrentVehicleAssociation(a.id(),a.vehicleId(),a.effectiveFrom())).orElse(null);return new DeviceResponse(d.id(),d.externalReference(),d.providerAlias(),d.hardwareSerialReference(),d.lifecycle().name(),d.registeredAt(),d.lastSeenAt(),d.version(),binding,association);}
 private static String mask(String value){return value.length()<5?"****":"****"+value.substring(value.length()-4);}
 public record DeviceRequest(@NotBlank @Size(max=160)String externalDeviceReference,@NotBlank @Size(max=80)String providerAlias,@Size(max=160)String hardwareSerialReference){}
 public record DeviceUpdate(@NotBlank @Size(max=160)String externalDeviceReference,@Size(max=160)String hardwareSerialReference,@PositiveOrZero long version){}
 public record VersionRequest(@PositiveOrZero long version){}
 public record AssociationRequest(@NotNull UUID vehicleId,@NotNull Instant effectiveFrom){}
 public record EndAssociationRequest(@NotNull Instant effectiveTo){}
 public record DeviceResponse(UUID id,String externalReference,String providerAlias,String hardwareSerialReference,String lifecycle,Instant registeredAt,Instant lastSeenAt,long version,CurrentProviderBinding currentProviderBinding,CurrentVehicleAssociation currentVehicleAssociation){}
 public record CurrentProviderBinding(UUID bindingId,UUID providerConnectionId,String bindingLifecycle,long bindingVersion,String providerDisplayName,String providerType,String providerAlias,String connectionLifecycle,String maskedExternalDeviceReference,Map<String,String> safeConfiguration){}
 public record CurrentVehicleAssociation(UUID associationId,UUID vehicleId,Instant effectiveFrom){}
}
