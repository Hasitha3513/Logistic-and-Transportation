package com.transportlogistics.app.integration.adapters.inbound.web.mappers;

import com.transportlogistics.app.integration.adapters.inbound.web.dto.request.CreateIntegrationRequest;
import com.transportlogistics.app.integration.adapters.inbound.web.dto.request.IntegrationMappingRequest;
import com.transportlogistics.app.integration.adapters.inbound.web.dto.request.UpdateIntegrationRequest;
import com.transportlogistics.app.integration.adapters.inbound.web.dto.response.IntegrationExchangeResponse;
import com.transportlogistics.app.integration.adapters.inbound.web.dto.response.IntegrationPageResponse;
import com.transportlogistics.app.integration.adapters.inbound.web.dto.response.IntegrationResponse;
import com.transportlogistics.app.integration.adapters.inbound.web.dto.response.IntegrationTestResponse;
import com.transportlogistics.app.integration.ports.inbound.IntegrationManagementUseCase;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface IntegrationWebMapper {
    default IntegrationManagementUseCase.CreateCommand toCommand(CreateIntegrationRequest source) {
        return new IntegrationManagementUseCase.CreateCommand(source.name(), source.type(), source.protocol(),
            source.direction(), source.endpointAlias(), source.credentialReference(), source.dataClassification(),
            toCommand(source.mapping()));
    }

    default IntegrationManagementUseCase.UpdateCommand toCommand(UpdateIntegrationRequest source) {
        return new IntegrationManagementUseCase.UpdateCommand(source.name(), source.endpointAlias(),
            source.credentialReference(), source.version(), toCommand(source.mapping()));
    }

    default IntegrationManagementUseCase.MappingCommand toCommand(IntegrationMappingRequest source) {
        return new IntegrationManagementUseCase.MappingCommand(source.mappingKey(), source.sourceContract(),
            source.sourceVersion(), source.targetSchema(), source.targetVersion(), source.rules().stream()
                .map(rule -> new com.transportlogistics.app.integration.domain.model.IntegrationMapping.Rule(
                    rule.sourceField(), rule.targetField(), rule.defaultValue(), rule.format(), rule.omitIfNull(),
                    rule.required())).toList());
    }

    default IntegrationResponse toResponse(IntegrationManagementUseCase.IntegrationView source) {
        var configuration = source.configuration();
        var mapping = source.mapping();
        var rules = mapping.rules().stream().map(rule -> new IntegrationResponse.RuleResponse(rule.sourceField(),
            rule.targetField(), rule.defaultValue(), rule.format(), rule.omitIfNull(), rule.required())).toList();
        var mappingResponse = new IntegrationResponse.MappingResponse(mapping.id(), mapping.mappingKey(),
            mapping.mappingVersion(), mapping.sourceContract(), mapping.sourceVersion(), mapping.targetSchema(),
            mapping.targetVersion(), mapping.definitionHash(), mapping.lifecycle(), rules);
        boolean configured = configuration.credentialReference() != null;
        return new IntegrationResponse(configuration.id(), configuration.name(), configuration.type(),
            configuration.protocol(), configuration.direction(), configuration.endpointAlias(), configured,
            configured ? "Configured credential" : null, configuration.dataClassification(),
            configuration.retryPolicy(), configuration.lifecycle(), configuration.health(),
            configuration.lastTestedAt(), configuration.lastSuccessfulExchangeAt(), configuration.version(),
            configuration.createdAt(), configuration.updatedAt(), mappingResponse);
    }

    default IntegrationTestResponse toResponse(IntegrationManagementUseCase.TestResult source) {
        return new IntegrationTestResponse(toResponse(source.integration()), source.success(), source.code(),
            source.testedAt());
    }

    default IntegrationExchangeResponse toResponse(IntegrationManagementUseCase.ExchangeView source) {
        var exchange = source.exchange();
        var attempts = source.attempts().stream().map(attempt -> new IntegrationExchangeResponse.AttemptResponse(
            attempt.attemptNumber(), attempt.startedAt(), attempt.completedAt(), attempt.latencyMillis(),
            attempt.outcome(), attempt.errorCode(), attempt.externalCorrelationId(), attempt.targetFilename())).toList();
        return new IntegrationExchangeResponse(exchange.id(), exchange.sourceEventId(), exchange.sourceEventType(),
            exchange.mappingVersionId(), exchange.mappingDefinitionHash(), exchange.payloadHash(), exchange.status(),
            exchange.attemptCount(), exchange.nextAttemptAt(),
            exchange.externalCorrelationId(), exchange.targetFilename(), exchange.lastErrorCode(), exchange.createdAt(),
            exchange.updatedAt(), exchange.completedAt(), attempts);
    }

    default IntegrationPageResponse<IntegrationResponse> toIntegrationPage(
            IntegrationManagementUseCase.PageResult<IntegrationManagementUseCase.IntegrationView> source) {
        return new IntegrationPageResponse<>(source.content().stream().map(this::toResponse).toList(), source.page(),
            source.size(), source.totalElements(), source.totalPages());
    }

    default IntegrationPageResponse<IntegrationExchangeResponse> toExchangePage(
            IntegrationManagementUseCase.PageResult<IntegrationManagementUseCase.ExchangeView> source) {
        return new IntegrationPageResponse<>(source.content().stream().map(this::toResponse).toList(), source.page(),
            source.size(), source.totalElements(), source.totalPages());
    }

}
