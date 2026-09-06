package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.request;

import com.transportlogistics.app.fuel.domain.model.FuelExceptionCase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public final class FuelExceptionRequests {
    private FuelExceptionRequests() {}
    public record Create(@NotNull FuelExceptionCase.Category category,@NotBlank String sourceType,@NotNull UUID sourceId,
                         @NotNull FuelExceptionCase.Impact impact,OffsetDateTime occurredAt,@NotBlank @Size(max=500) String summary,
                         Map<String,String> safeMetadata,UUID vehicleId,UUID driverId,UUID tripId,UUID cardId,UUID tankId) {}
    public record VersionedReason(long version,@NotBlank @Size(max=500) String reason) {}
    public record Evidence(@NotBlank String evidenceType,String sourceType,UUID sourceId,@NotBlank @Size(max=500) String summary,Map<String,String> safeSnapshot) {}
    public record Note(@NotBlank @Size(max=1000) String text) {}
    public record Correction(@NotBlank String correctionType,@NotNull Map<String,String> ownerCommand,boolean changesFinancialFact) {}
    public record Resolve(long version,@NotNull FuelExceptionCase.Outcome outcome,@NotBlank @Size(max=500) String reason) {}
}
