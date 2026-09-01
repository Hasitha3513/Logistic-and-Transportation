package com.transportlogistics.app.delivery.domain.events;

import com.transportlogistics.app.delivery.domain.model.EtaStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliveryEtaCalculatedEvent(
        UUID eventId,
        UUID tenantId,
        String subjectType,
        UUID subjectId,
        OffsetDateTime estimatedArrivalAt,
        long totalDurationSeconds,
        long totalDistanceMeters,
        EtaStatus slaStatus,
        OffsetDateTime calculatedAt,
        String actor
) {
    public static DeliveryEtaCalculatedEvent of(
            UUID tenantId,
            String subjectType,
            UUID subjectId,
            OffsetDateTime estimatedArrivalAt,
            long totalDurationSeconds,
            long totalDistanceMeters,
            EtaStatus slaStatus,
            OffsetDateTime calculatedAt,
            String actor
    ) {
        return new DeliveryEtaCalculatedEvent(
                UUID.randomUUID(),
                tenantId,
                subjectType,
                subjectId,
                estimatedArrivalAt,
                totalDurationSeconds,
                totalDistanceMeters,
                slaStatus,
                calculatedAt,
                actor
        );
    }
}
