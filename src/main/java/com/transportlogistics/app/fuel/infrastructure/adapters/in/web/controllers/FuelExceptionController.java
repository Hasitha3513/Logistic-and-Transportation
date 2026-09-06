package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.controllers;

import com.transportlogistics.app.fuel.application.ports.in.FuelExceptionUseCase;
import com.transportlogistics.app.fuel.domain.model.FuelExceptionCase;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.request.FuelExceptionRequests;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response.FuelExceptionResponse;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.mappers.FuelExceptionWebMapper;
import com.transportlogistics.app.tenancy.CurrentTenant;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/v1/fuel/exceptions")
public class FuelExceptionController {
 private final FuelExceptionUseCase useCase;private final CurrentTenant tenants;private final FuelExceptionWebMapper mapper;
 public FuelExceptionController(FuelExceptionUseCase useCase,CurrentTenant tenants,FuelExceptionWebMapper mapper){this.useCase=useCase;this.tenants=tenants;this.mapper=mapper;}
 @GetMapping public List<FuelExceptionResponse> list(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int limit,@RequestParam(required=false)FuelExceptionCase.Category category,@RequestParam(required=false)FuelExceptionCase.Lifecycle lifecycle,@RequestParam(required=false)String sourceType,@RequestParam(required=false)UUID vehicleId,@RequestParam(required=false)UUID driverId,@RequestParam(required=false)UUID cardId,@RequestParam(required=false)UUID tankId,@RequestParam(required=false)OffsetDateTime occurredFrom,@RequestParam(required=false)OffsetDateTime occurredTo,@RequestParam(required=false)Boolean reviewRequired,@RequestParam(required=false)FuelExceptionCase.HandoffStatus handoffStatus,@RequestParam(defaultValue="createdAt")String sort,@RequestParam(defaultValue="desc")String direction){var s=new FuelExceptionUseCase.Search(page,limit,category,lifecycle,sourceType,vehicleId,driverId,cardId,tankId,occurredFrom,occurredTo,reviewRequired,handoffStatus,sort,direction);return useCase.list(context().tenantId(),s).stream().map(mapper::toResponse).toList();}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public FuelExceptionResponse create(@Valid @RequestBody FuelExceptionRequests.Create r){return mapper.toResponse(useCase.create(context(),new FuelExceptionUseCase.Create(r.category(),r.sourceType(),r.sourceId(),r.impact(),r.occurredAt(),r.summary(),r.safeMetadata(),r.vehicleId(),r.driverId(),r.tripId(),r.cardId(),r.tankId())));}
 @GetMapping("/{id}") public FuelExceptionResponse.Detail detail(@PathVariable UUID id){return mapper.toResponse(useCase.detail(context().tenantId(),id));}
 @PostMapping("/{id}/review") public FuelExceptionResponse review(@PathVariable UUID id,@Valid @RequestBody FuelExceptionRequests.VersionedReason r){return mapper.toResponse(useCase.review(context(),id,versioned(r)));}
 @GetMapping("/{id}/evidence") public List<FuelExceptionUseCase.Evidence> evidence(@PathVariable UUID id){return useCase.detail(context().tenantId(),id).evidence();}
 @PostMapping("/{id}/evidence") @ResponseStatus(HttpStatus.CREATED) public FuelExceptionUseCase.Evidence evidence(@PathVariable UUID id,@Valid @RequestBody FuelExceptionRequests.Evidence r){return useCase.addEvidence(context(),id,new FuelExceptionUseCase.AddEvidence(r.evidenceType(),r.sourceType(),r.sourceId(),r.summary(),r.safeSnapshot()));}
 @PostMapping("/{id}/notes") @ResponseStatus(HttpStatus.CREATED) public FuelExceptionUseCase.Note note(@PathVariable UUID id,@Valid @RequestBody FuelExceptionRequests.Note r){return useCase.addNote(context(),id,new FuelExceptionUseCase.Text(r.text()));}
 @PostMapping("/{id}/corrections") @ResponseStatus(HttpStatus.CREATED) public FuelExceptionUseCase.Correction correction(@PathVariable UUID id,@Valid @RequestBody FuelExceptionRequests.Correction r){return useCase.requestCorrection(context(),id,new FuelExceptionUseCase.RequestCorrection(r.correctionType(),r.ownerCommand(),r.changesFinancialFact()));}
 @PostMapping("/{id}/corrections/{correctionId}/approve") public FuelExceptionUseCase.Correction approve(@PathVariable UUID id,@PathVariable UUID correctionId,@Valid @RequestBody FuelExceptionRequests.VersionedReason r){return useCase.approve(context(),id,correctionId,versioned(r));}
 @PostMapping("/{id}/corrections/{correctionId}/reject") public FuelExceptionUseCase.Correction reject(@PathVariable UUID id,@PathVariable UUID correctionId,@Valid @RequestBody FuelExceptionRequests.VersionedReason r){return useCase.reject(context(),id,correctionId,versioned(r));}
 @PostMapping("/{id}/resolve") public FuelExceptionResponse resolve(@PathVariable UUID id,@Valid @RequestBody FuelExceptionRequests.Resolve r){return mapper.toResponse(useCase.resolve(context(),id,new FuelExceptionUseCase.Resolve(r.version(),r.outcome(),r.reason())));}
 @PostMapping("/{id}/escalate") public FuelExceptionResponse escalate(@PathVariable UUID id,@Valid @RequestBody FuelExceptionRequests.VersionedReason r){return mapper.toResponse(useCase.escalate(context(),id,versioned(r)));}
 @GetMapping("/{id}/history") public List<FuelExceptionUseCase.History> history(@PathVariable UUID id){return useCase.detail(context().tenantId(),id).history();}
 private FuelExceptionUseCase.VersionedReason versioned(FuelExceptionRequests.VersionedReason r){return new FuelExceptionUseCase.VersionedReason(r.version(),r.reason());}
 private FuelExceptionUseCase.Context context(){var c=tenants.required();return new FuelExceptionUseCase.Context(c.tenantId(),c.actorId(),c.username(),c.correlationId());}
}
