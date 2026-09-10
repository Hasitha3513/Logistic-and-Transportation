package com.transportlogistics.app.tracking.domain.geofence;

import com.transportlogistics.app.tracking.domain.TrackingModels.Ordering;
import com.transportlogistics.app.tracking.domain.TrackingModels.Trust;
import java.time.Instant;
import java.util.UUID;

public record GeofencePosition(UUID tenantId, UUID positionId, UUID vehicleId,
                               Instant sourceTimestamp, Wgs84Coordinate coordinate,
                               Trust trust, Ordering ordering, boolean duplicate) {
}
