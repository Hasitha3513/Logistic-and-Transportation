package com.transportlogistics.app.billing.adapters.inbound.web.dto.request;
import com.transportlogistics.app.billing.domain.TransportBillingRecord.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
public final class BillingRequests {private BillingRequests(){}
 public record Create(@NotNull Source.SourceType sourceType,@NotNull UUID sourceId,@NotBlank @Pattern(regexp="[A-Z]{3}")String currency,UUID replacementOfRecordId){}
 public record Version(@PositiveOrZero long version){}
 public record Reason(@PositiveOrZero long version,@NotBlank @Size(max=160)String reason){}
 public record Line(UUID id,@NotNull LineCategory category,@NotBlank @Size(max=80)String reasonCode,@NotBlank @Size(max=160)String provenance,@NotNull @DecimalMin("0.0000")BigDecimal quantity,@NotNull @DecimalMin("0.0000")BigDecimal unitRate,@NotNull @DecimalMin("0.00")BigDecimal amount){}
 public record Tax(@NotNull TaxStatus status,@Size(max=80)String category,@Size(max=120)String jurisdictionReference,BigDecimal taxableAmount,BigDecimal rate,BigDecimal taxAmount,@Size(max=120)String exemptionReference,@Size(max=160)String provenance,@Pattern(regexp="[0-9a-f]{64}")String snapshotHash){}
 public record CostCentre(UUID id,@NotBlank @Size(max=80)String code,@NotNull @DecimalMin("0.0001") @DecimalMax("100.0000")BigDecimal allocationPercent,@Size(max=240)String description,@NotBlank @Size(max=160)String source){}
 public record Replacement(@NotEmpty List<@Valid Line> lines,@NotNull @Valid Tax tax,@NotEmpty List<@Valid CostCentre> costCentres,@PositiveOrZero long version){}
}
