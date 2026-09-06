package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.fuel.application.ports.in.FuelExceptionUseCase;
import com.transportlogistics.app.fuel.domain.model.FuelExceptionCase;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record FuelExceptionResponse(UUID id,FuelExceptionCase.Category category,FuelExceptionCase.Lifecycle lifecycle,
 FuelExceptionCase.Impact impact,String sourceType,UUID sourceId,String summary,Map<String,String> safeMetadata,
 UUID vehicleId,UUID driverId,UUID tripId,UUID cardId,UUID tankId,OffsetDateTime occurredAt,boolean reviewRequired,
 FuelExceptionCase.HandoffStatus handoffStatus,FuelExceptionCase.Outcome resolutionOutcome,String resolutionReason,
 long version,OffsetDateTime createdAt,OffsetDateTime updatedAt) {
 public record Detail(FuelExceptionResponse value,List<FuelExceptionUseCase.Evidence> evidence,List<FuelExceptionUseCase.Note> notes,List<FuelExceptionUseCase.Correction> corrections,List<FuelExceptionUseCase.History> history){}
}
