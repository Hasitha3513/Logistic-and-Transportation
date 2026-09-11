package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public final class FuelCardRequests {
    private FuelCardRequests() {}
    public record Create(@NotNull UUID providerId, @NotBlank @Size(max=100) String alias,
                         @NotBlank @Size(max=255) String providerCardReference,
                         @NotBlank @Size(max=32) String maskedIdentifier,
                         @Pattern(regexp="[0-9]{4}") String lastFour,
                         @Min(1) @Max(12) int expiryMonth, @Min(2000) @Max(9999) int expiryYear) {}
    public record Command(@PositiveOrZero long version, @Size(max=500) String reason) {}
    public record Update(@NotBlank @Size(max=100) String alias, @Min(1) @Max(12) int expiryMonth,
                         @Min(2000) @Max(9999) int expiryYear, @PositiveOrZero long version) {}
    public record Binding(@NotBlank String bindingType, @NotNull UUID bindingId,
                          @PositiveOrZero long version, @NotBlank @Size(max=500) String reason) {}
    public record Restriction(@NotBlank @Size(min=3,max=3) String currency,
                              @NotNull @DecimalMin("0.01") BigDecimal maxTransactionAmount,
                              @NotNull @DecimalMin("0.01") BigDecimal maxDailyAmount,
                              @NotNull @DecimalMin("0.01") BigDecimal maxMonthlyAmount,
                              @NotNull @DecimalMin("0.0001") BigDecimal maxDailyLitres,
                              @NotEmpty Set<@NotBlank String> allowedFuelTypes,
                              Set<@NotBlank String> allowedStationReferences,
                              @PositiveOrZero long version, @NotBlank @Size(max=500) String reason) {}
    public record Reconciliation(UUID purchaseId, @PositiveOrZero long version,
                                 @NotBlank @Size(max=500) String reason) {}
}
