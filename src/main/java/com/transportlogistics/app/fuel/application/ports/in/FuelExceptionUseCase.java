package com.transportlogistics.app.fuel.application.ports.in;

import com.transportlogistics.app.fuel.domain.model.FuelExceptionCase;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface FuelExceptionUseCase {
    FuelExceptionCase create(Context context, Create command);
    List<FuelExceptionCase> list(UUID tenantId, Search search);
    Detail detail(UUID tenantId, UUID id);
    FuelExceptionCase review(Context context, UUID id, VersionedReason command);
    Evidence addEvidence(Context context, UUID id, AddEvidence command);
    Note addNote(Context context, UUID id, Text command);
    Correction requestCorrection(Context context, UUID id, RequestCorrection command);
    Correction approve(Context context, UUID id, UUID correctionId, VersionedReason command);
    Correction reject(Context context, UUID id, UUID correctionId, VersionedReason command);
    FuelExceptionCase resolve(Context context, UUID id, Resolve command);
    FuelExceptionCase escalate(Context context, UUID id, VersionedReason command);

    record Context(UUID tenantId, UUID actorId, String username, String correlationId) {}
    record Create(FuelExceptionCase.Category category, String sourceType, UUID sourceId,
                  FuelExceptionCase.Impact impact, OffsetDateTime occurredAt, String summary,
                  Map<String,String> safeMetadata, UUID vehicleId, UUID driverId, UUID tripId,
                  UUID cardId, UUID tankId) {}
    record Search(int page, int limit, FuelExceptionCase.Category category, FuelExceptionCase.Lifecycle lifecycle,
                  String sourceType, UUID vehicleId, UUID driverId, UUID cardId, UUID tankId,
                  OffsetDateTime occurredFrom, OffsetDateTime occurredTo, Boolean reviewRequired,
                  FuelExceptionCase.HandoffStatus handoffStatus, String sort, String direction) {}
    record VersionedReason(long version, String reason) {}
    record AddEvidence(String evidenceType, String sourceType, UUID sourceId, String summary, Map<String,String> safeSnapshot) {}
    record Text(String text) {}
    record RequestCorrection(String correctionType, Map<String,String> ownerCommand, boolean changesFinancialFact) {}
    record Resolve(long version, FuelExceptionCase.Outcome outcome, String reason) {}
    record Evidence(UUID id, String evidenceType, String sourceType, UUID sourceId, String summary,
                    Map<String,String> safeSnapshot, UUID addedBy, OffsetDateTime createdAt) {}
    record Note(UUID id, String note, UUID addedBy, OffsetDateTime createdAt) {}
    record Correction(UUID id, String correctionType, Map<String,String> ownerCommand, boolean changesFinancialFact,
                      String status, UUID requestedBy, UUID reviewedBy, String reviewReason,
                      String ownerResultReference, String failureCode, long version,
                      OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
    record History(UUID id, String action, String fromLifecycle, String toLifecycle, String detail,
                   UUID actorId, OffsetDateTime createdAt) {}
    record Detail(FuelExceptionCase value, List<Evidence> evidence, List<Note> notes,
                  List<Correction> corrections, List<History> history) {}
}
