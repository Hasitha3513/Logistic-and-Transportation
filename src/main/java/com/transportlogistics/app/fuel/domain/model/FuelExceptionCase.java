package com.transportlogistics.app.fuel.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record FuelExceptionCase(UUID id, UUID tenantId, Category category, Lifecycle lifecycle, Impact impact,
                                String sourceType, UUID sourceId, UUID sourceEventId, String summary,
                                Map<String, String> safeMetadata, UUID vehicleId, UUID driverId, UUID tripId,
                                UUID cardId, UUID tankId, OffsetDateTime occurredAt, boolean reviewRequired,
                                HandoffStatus handoffStatus, Outcome resolutionOutcome, String resolutionReason,
                                UUID createdBy, UUID resolvedBy, long version, OffsetDateTime createdAt,
                                OffsetDateTime updatedAt) {
    public FuelExceptionCase {
        safeMetadata = safeMetadata == null ? Map.of() : Map.copyOf(safeMetadata);
    }

    public void requireLifecycle(Lifecycle expected) {
        if (lifecycle != expected) throw rule("FUEL_EXCEPTION_INVALID_STATE");
    }

    public static BusinessRuleException rule(String code) {
        return new BusinessRuleException(code, code);
    }

    public enum Category { SUSPECTED_FUEL_LOSS, INCORRECT_READING, SUDDEN_PRICE_CHANGE, EMERGENCY_REFUEL, FUEL_CARD_POLICY_DEVIATION, NEGATIVE_BUNKER_BALANCE }
    public enum Lifecycle { OPEN, UNDER_REVIEW, CORRECTION_PENDING, AWAITING_APPROVAL, RESOLVED }
    public enum Impact { LOW, MEDIUM, HIGH, CRITICAL }
    public enum Outcome { NO_ACTION_REQUIRED, CORRECTION_APPLIED, RECONCILED, EMERGENCY_REFUEL_ACCEPTED, REFERRED_TO_OPERATIONS }
    public enum HandoffStatus { NOT_REQUIRED, PENDING, PUBLISHED, ACCEPTED, FAILED }
}
