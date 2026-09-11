package com.transportlogistics.app.tracking.adapters.inbound.web.dto.request;

import com.transportlogistics.app.tracking.domain.geofence.GeofenceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public final class GeofenceRequests {
    private GeofenceRequests() {
    }

    public record Coordinate(@NotNull Double longitude, @NotNull Double latitude) {
    }

    public record AlertPolicy(boolean alertOnEntry, boolean alertOnExit) {
    }

    public record Create(
            @NotBlank @Size(max = 160) String name,
            @NotNull GeofenceType type,
            UUID locationId,
            @NotNull @Size(min = 3, max = 100) List<@Valid Coordinate> polygon,
            @NotNull @Valid AlertPolicy alertPolicy) {
    }

    public record Update(
            @PositiveOrZero long expectedVersion,
            @NotBlank @Size(max = 160) String name,
            @NotNull GeofenceType type,
            UUID locationId,
            @NotNull @Size(min = 3, max = 100) List<@Valid Coordinate> polygon,
            @NotNull @Valid AlertPolicy alertPolicy) {
    }

    public record Version(@PositiveOrZero long expectedVersion) {
    }

    public record Reason(@PositiveOrZero long expectedVersion,
                         @NotBlank @Size(max = 300) String reason) {
    }
}
