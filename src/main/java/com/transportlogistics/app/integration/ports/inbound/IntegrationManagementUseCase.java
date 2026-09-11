package com.transportlogistics.app.integration.ports.inbound;

import com.transportlogistics.app.integration.domain.model.IntegrationConfiguration;
import com.transportlogistics.app.integration.domain.model.IntegrationExchange;
import com.transportlogistics.app.integration.domain.model.IntegrationExchangeAttempt;
import com.transportlogistics.app.integration.domain.model.IntegrationMapping;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface IntegrationManagementUseCase {
    IntegrationView create(Context context, CreateCommand command);
    IntegrationView update(Context context, UUID id, UpdateCommand command);
    IntegrationView get(Context context, UUID id);
    PageResult<IntegrationView> list(Context context, int page, int size);
    TestResult test(Context context, UUID id);
    IntegrationView enable(Context context, UUID id, long version);
    IntegrationView disable(Context context, UUID id, long version);
    PageResult<ExchangeView> exchanges(Context context, UUID id, int page, int size);

    record Context(UUID tenantId, String actor, String correlationId) {}
    record MappingCommand(String mappingKey, String sourceContract, int sourceVersion, String targetSchema,
                          int targetVersion, List<IntegrationMapping.Rule> rules) {}
    record CreateCommand(String name, IntegrationConfiguration.Type type, IntegrationConfiguration.Protocol protocol,
                         IntegrationConfiguration.Direction direction, String endpointAlias,
                         String credentialReference, IntegrationConfiguration.DataClassification dataClassification,
                         MappingCommand mapping) {}
    record UpdateCommand(String name, String endpointAlias, String credentialReference, long version,
                         MappingCommand mapping) {}
    record IntegrationView(IntegrationConfiguration configuration, IntegrationMapping mapping) {}
    record TestResult(IntegrationView integration, boolean success, String code, OffsetDateTime testedAt) {}
    record ExchangeView(IntegrationExchange exchange, List<IntegrationExchangeAttempt> attempts) {}
    record PageResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
}
