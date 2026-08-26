package com.transportlogistics.app.freight.manifest.adapters.inbound.web.dto.request;
import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.util.UUID;
public record CargoManifestItemRequest(@NotNull @PositiveOrZero Long version,@NotNull UUID freightOrderLineId,
 @NotBlank @Size(max=500) String description,@NotNull @DecimalMin(value="0",inclusive=false) BigDecimal quantity,
 @NotBlank @Size(max=500) String packingInformation,
 @NotBlank @Size(max=120) @Pattern(regexp="[A-Za-z0-9][A-Za-z0-9_.-]*") String commodityClassification,
 boolean customsApplicable,@Size(max=1000) String customsInformation,boolean hazardous,
 @Size(max=120) @Pattern(regexp="[A-Za-z0-9][A-Za-z0-9_.-]*") String hazardousClassification,
 @Size(max=1000) String hazardousDetails,Boolean fragile,Boolean temperatureSensitive) { }
