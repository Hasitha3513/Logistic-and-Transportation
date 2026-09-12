package com.transportlogistics.app.tracking.adapters.inbound.web.dto.request;

import com.transportlogistics.app.tracking.domain.speed.SpeedRule;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public final class SpeedMonitoringRequests {
    private SpeedMonitoringRequests() { }
    public record Create(@NotBlank @Size(max=120) String name, @NotNull SpeedRule.Scope scope,
                         UUID routeId, @Size(max=120) String routeVersion,
                         @NotNull @DecimalMin(value="0", inclusive=false) @DecimalMax("400") BigDecimal thresholdKph) { }
    public record Update(@Positive long expectedVersion, @NotBlank @Size(max=120) String name,
                         UUID routeId, @Size(max=120) String routeVersion,
                         @NotNull @DecimalMin(value="0", inclusive=false) @DecimalMax("400") BigDecimal thresholdKph) { }
    public record Version(@Positive long expectedVersion) { }
    public record Reason(@Positive long expectedVersion, @NotBlank @Size(max=300) String reason) { }
}
