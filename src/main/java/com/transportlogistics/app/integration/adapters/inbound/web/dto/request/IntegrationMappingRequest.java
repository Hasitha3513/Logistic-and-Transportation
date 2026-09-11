package com.transportlogistics.app.integration.adapters.inbound.web.dto.request;

import com.transportlogistics.app.integration.domain.model.IntegrationMapping;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record IntegrationMappingRequest(
        @NotBlank @Size(max = 100) String mappingKey,
        @NotBlank @Size(max = 100) String sourceContract,
        @Min(1) int sourceVersion,
        @NotBlank @Size(max = 100) String targetSchema,
        @Min(1) int targetVersion,
        @NotEmpty @Size(max = 100) List<@Valid RuleRequest> rules
) {
    public record RuleRequest(
            @Size(max = 100) String sourceField,
            @NotBlank @Size(max = 100) String targetField,
            @Size(max = 200) String defaultValue,
            @NotNull IntegrationMapping.Format format,
            boolean omitIfNull,
            boolean required
    ) {}
}
