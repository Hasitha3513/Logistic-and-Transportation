package com.transportlogistics.app.delivery.ports.outbound;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface DeliverySlotAvailabilityPort {
    SlotAvailability checkAvailability(UUID destinationLocationId, OffsetDateTime requestedFrom, OffsetDateTime requestedTo);

    record SlotAvailability(boolean available, String reasonCode) {}
}
