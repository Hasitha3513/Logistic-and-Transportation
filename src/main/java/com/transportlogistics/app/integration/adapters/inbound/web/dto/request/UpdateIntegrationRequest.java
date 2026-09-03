package com.transportlogistics.app.integration.adapters.inbound.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateIntegrationRequest(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Size(max = 80) String endpointAlias,
        @Size(max = 160) String credentialReference,
        @Min(0) long version,
        @NotNull @Valid IntegrationMappingRequest mapping
) {}
