package com.transportlogistics.app.tracking.adapters.inbound.web.dto.request;

import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.OverlayType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class JourneyReplayRequests {
    private JourneyReplayRequests() { }

    public record Query(UUID vehicleId, UUID tripId, Instant from, Instant to,
                        @Min(1) @Max(2000) Integer limit, String cursor) { }

    public record StopQuery(UUID vehicleId, UUID tripId, Instant from, Instant to,
                            @Min(1) @Max(500) Integer limit, String cursor) { }

    public record IncidentQuery(UUID vehicleId, UUID tripId, Instant from, Instant to,
                                @Min(1) @Max(2000) Integer limit, String cursor,
                                Set<OverlayType> types) { }
}
