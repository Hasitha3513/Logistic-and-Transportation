package com.transportlogistics.app.tracking.domain.gpsedge;

import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Ordering;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.ReliabilityState;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Trust;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Immutable minimized evidence for one GPS-exception episode transition. */
public record GpsExceptionEvidence(
        UUID id,
        UUID tenantId,
        UUID episodeId,
        String evidenceIdentity,
        UUID telemetryHistoryId,
        Instant telemetrySourceTimestamp,
        Instant assessedAt,
        Trust trust,
        Ordering ordering,
        ReliabilityState reliabilityState,
        String qualityCodes,
        Transition transition,
        Instant createdAt) {
    public GpsExceptionEvidence {
        Objects.requireNonNull(id);
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(episodeId);
        Objects.requireNonNull(evidenceIdentity);
        Objects.requireNonNull(assessedAt);
        Objects.requireNonNull(trust);
        Objects.requireNonNull(ordering);
        Objects.requireNonNull(reliabilityState);
        Objects.requireNonNull(qualityCodes);
        Objects.requireNonNull(transition);
        Objects.requireNonNull(createdAt);
        if (evidenceIdentity.length() != 64 || qualityCodes.isBlank() || qualityCodes.length() > 400
                || (telemetryHistoryId == null) != (telemetrySourceTimestamp == null)) {
            throw new GpsEdgeCaseException("INVALID_EVIDENCE", "GPS exception evidence is invalid");
        }
    }

    public enum Transition {
        OPENED, OBSERVED, RECOVERING, RESOLVED
    }
}
