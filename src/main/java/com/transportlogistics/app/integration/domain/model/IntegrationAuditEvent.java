package com.transportlogistics.app.integration.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record IntegrationAuditEvent(
        UUID id, UUID tenantId, String actor, Action action, String targetType, UUID targetId,
        Outcome outcome, String safeCode, String beforeHash, String afterHash, String correlationId,
        OffsetDateTime occurredAt
) {
    public enum Action {
        CREATE, UPDATE, ENABLE, DISABLE, CREDENTIAL_REFERENCE_CHANGE, MAPPING_VERSION_CREATE,
        MAPPING_ACTIVATE, TEST_CONNECTION, DURABLE_FACT_ACCEPT, ATTEMPT, TERMINAL_FAILURE, RECONCILIATION_VIEW
    }
    public enum Outcome { SUCCESS, FAILURE, DENIED }
}
