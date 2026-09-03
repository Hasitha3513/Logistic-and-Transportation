package com.transportlogistics.app.integration.adapters.inbound.web.dto.request;

import com.transportlogistics.app.integration.domain.model.IntegrationConfiguration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateIntegrationRequest(
        @NotBlank @Size(max = 160) String name,
        @NotNull IntegrationConfiguration.Type type,
        @NotNull IntegrationConfiguration.Protocol protocol,
        @NotNull IntegrationConfiguration.Direction direction,
        @NotBlank @Size(max = 80) String endpointAlias,
        @Size(max = 160) String credentialReference,
        @NotNull IntegrationConfiguration.DataClassification dataClassification,
        @NotNull @Valid IntegrationMappingRequest mapping
) {}
