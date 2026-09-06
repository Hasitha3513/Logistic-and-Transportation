package com.transportlogistics.app.fuel.application.ports.out;

import com.transportlogistics.app.fuel.application.ports.in.FuelExceptionUseCase;
import com.transportlogistics.app.fuel.domain.model.FuelExceptionCase;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FuelExceptionStore {
    FuelExceptionCase insert(FuelExceptionCase value);
    FuelExceptionCase update(UUID tenantId, UUID id, long version, FuelExceptionCase.Lifecycle lifecycle,
                             boolean reviewRequired, FuelExceptionCase.HandoffStatus handoffStatus,
                             FuelExceptionCase.Outcome outcome, String resolutionReason, UUID resolvedBy,
                             OffsetDateTime now);
    Optional<FuelExceptionCase> find(UUID tenantId, UUID id);
    List<FuelExceptionCase> search(UUID tenantId, FuelExceptionUseCase.Search search);
    FuelExceptionUseCase.Evidence evidence(UUID tenantId, UUID exceptionId, FuelExceptionUseCase.AddEvidence value, UUID actor, OffsetDateTime now);
    FuelExceptionUseCase.Note note(UUID tenantId, UUID exceptionId, String note, UUID actor, OffsetDateTime now);
    FuelExceptionUseCase.Correction correction(UUID tenantId, UUID exceptionId, FuelExceptionUseCase.RequestCorrection value, UUID actor, String status, OffsetDateTime now);
    Optional<FuelExceptionUseCase.Correction> correction(UUID tenantId, UUID exceptionId, UUID correctionId);
    FuelExceptionUseCase.Correction reviewCorrection(UUID tenantId, UUID exceptionId, UUID correctionId, long version, String status, UUID actor, String reason, OffsetDateTime now);
    FuelExceptionUseCase.Correction correctionResult(UUID tenantId, UUID exceptionId, UUID correctionId, long version, String status, String ownerReference, String failureCode, OffsetDateTime now);
    void history(UUID tenantId, UUID exceptionId, String action, String from, String to, String detail, UUID actor, OffsetDateTime now);
    FuelExceptionUseCase.Detail detail(FuelExceptionCase value);
    UUID handoff(UUID tenantId, UUID exceptionId, String reason, OffsetDateTime now);
    boolean sourceExists(UUID tenantId, String sourceType, UUID sourceId);
}
