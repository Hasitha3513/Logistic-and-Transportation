package com.transportlogistics.app.trip.infrastructure.adapters.in.web.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TripHistoryResponse(UUID id,
                                  UUID tripId,
                                  String fromStatus,
                                  String toStatus,
                                  String action,
                                  UUID vehicleId,
                                  UUID driverId,
                                  String licenseClass,
                                  String actor,
                                  String details,
                                  OffsetDateTime occurredAt) {
}
