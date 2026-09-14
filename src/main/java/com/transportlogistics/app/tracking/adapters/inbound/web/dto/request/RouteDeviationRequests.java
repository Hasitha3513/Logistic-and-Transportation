package com.transportlogistics.app.tracking.adapters.inbound.web.dto.request;

import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationReview;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public final class RouteDeviationRequests {
    private RouteDeviationRequests() { }
    public record CreateRule(@NotNull UUID routeId,
                             @NotBlank @Size(max = 120) String routeVersion,
                             @NotNull @DecimalMin("10") @DecimalMax("5000")
                             BigDecimal toleranceMeters) { }
    public record UpdateRule(@PositiveOrZero long expectedVersion,
                             @NotNull @DecimalMin("10") @DecimalMax("5000")
                             BigDecimal toleranceMeters) { }
    public record Version(@PositiveOrZero long expectedVersion) { }
    public record Lifecycle(@PositiveOrZero long expectedVersion,
                            @NotBlank @Size(max = 300) String reason) { }
    public record Review(@PositiveOrZero long expectedVersion,
                         @NotNull RouteDeviationReview.Reason reason,
                         @Size(max = 500) String note) { }
    public record CorrectReview(@PositiveOrZero long expectedVersion,
                                @NotNull RouteDeviationReview.Status status,
                                @NotNull RouteDeviationReview.Reason reason,
                                @Size(max = 500) String note) { }
}
