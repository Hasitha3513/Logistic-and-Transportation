package com.transportlogistics.app.freight.manifest.adapters.inbound.web.dto.request;
import jakarta.validation.constraints.*;
public record FinalizeCargoManifestRequest(@NotNull @PositiveOrZero Long version) { }
