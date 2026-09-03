package com.transportlogistics.app.operations;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OperationalExceptionFactV1Test {
    @Test
    void exposesOnlyTheFrozenDurableEnvelopeAndSafePayload() {
        UUID eventId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        var fact = new OperationalExceptionFactV1(eventId, tenantId,
            OperationalExceptionFactV1.SourceModule.ROUTING, "ACCIDENT", sourceId, OffsetDateTime.now(),
            OperationalExceptionFactV1.Severity.HIGH, OperationalExceptionFactV1.Category.SAFETY,
            "ROUTE_DISRUPTION_CREATED", Map.of("routeId", UUID.randomUUID().toString()), "correlation");

        assertThat(fact.eventType()).isEqualTo("OPERATIONAL_EXCEPTION_FACT_V1");
        assertThat(fact.durableConsumer()).isEqualTo("operations-exception-intake");
        assertThat(fact.version()).isEqualTo(1);
        assertThat(fact.payload()).containsOnlyKeys("sourceModule", "sourceType", "sourceId",
            "severityCandidate", "categoryCandidate", "summaryCode", "safeMetadata", "correlationId");
    }

    @Test
    void rejectsUnknownSourceTypesSummaryCodesAndMetadata() {
        assertThatThrownBy(() -> fact("UNKNOWN", "ROUTE_DISRUPTION_CREATED", Map.of()))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> fact("ACCIDENT", "OTHER", Map.of()))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> fact("ACCIDENT", "ROUTE_DISRUPTION_CREATED", Map.of("description", "private")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private OperationalExceptionFactV1 fact(String type, String summary, Map<String, String> metadata) {
        return new OperationalExceptionFactV1(UUID.randomUUID(), UUID.randomUUID(),
            OperationalExceptionFactV1.SourceModule.ROUTING, type, UUID.randomUUID(), OffsetDateTime.now(),
            OperationalExceptionFactV1.Severity.HIGH, OperationalExceptionFactV1.Category.SAFETY,
            summary, metadata, null);
    }
}
