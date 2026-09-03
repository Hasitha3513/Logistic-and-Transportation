package com.transportlogistics.app.integration.adapters.inbound.web.dto.response;

import com.transportlogistics.app.integration.domain.model.IntegrationConfiguration;
import com.transportlogistics.app.integration.domain.model.IntegrationMapping;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record IntegrationResponse(
        UUID id, String name, IntegrationConfiguration.Type type, IntegrationConfiguration.Protocol protocol,
        IntegrationConfiguration.Direction direction, String endpointAlias, boolean credentialConfigured,
        String credentialReferenceLabel, IntegrationConfiguration.DataClassification dataClassification,
        IntegrationConfiguration.RetryPolicy retryPolicy, IntegrationConfiguration.Lifecycle lifecycle,
        IntegrationConfiguration.Health health, OffsetDateTime lastTestedAt,
        OffsetDateTime lastSuccessfulExchangeAt, long version, OffsetDateTime createdAt, OffsetDateTime updatedAt,
        MappingResponse mapping
) {
    public record MappingResponse(
            UUID id, String mappingKey, int mappingVersion, String sourceContract, int sourceVersion,
            String targetSchema, int targetVersion, String definitionHash, IntegrationMapping.Lifecycle lifecycle,
            List<RuleResponse> rules
    ) {}

    public record RuleResponse(String sourceField, String targetField, String defaultValue,
                               IntegrationMapping.Format format, boolean omitIfNull, boolean required) {}
}
