package com.transportlogistics.app.operations.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OperationalExceptionCaseTest {
    private static final OffsetDateTime OPENED = OffsetDateTime.parse("2026-09-04T00:00:00Z");

    @Test
    void appliesFrozenHighSlaLifecycleAndClosureSegregation() {
        UUID resolver = UUID.randomUUID();
        var value = caseWith(OperationalExceptionCase.Severity.HIGH);

        assertThat(value.responseDueAt()).isEqualTo(OPENED.plusHours(1));
        assertThat(value.resolutionDueAt()).isEqualTo(OPENED.plusHours(8));
        assertThat(value.slaStatus(OPENED.plusHours(6))).isEqualTo(OperationalExceptionCase.SlaStatus.AT_RISK);
        value.acknowledge(OPENED.plusMinutes(5));
        value.start(OPENED.plusMinutes(6));
        value.resolve("Source correction verified", null, resolver, OPENED.plusHours(2));

        assertThatThrownBy(() -> value.close(resolver, true, OPENED.plusHours(3)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("different");
        value.close(UUID.randomUUID(), true, OPENED.plusHours(3));
        assertThat(value.status()).isEqualTo(OperationalExceptionCase.Status.CLOSED);
    }

    @Test
    void forbidsSkippedTransitionsAndRestartsResolutionSlaOnReopen() {
        var value = caseWith(OperationalExceptionCase.Severity.MEDIUM);
        assertThatThrownBy(() -> value.resolve("not allowed", null, UUID.randomUUID(), OPENED.plusHours(1)))
            .isInstanceOf(BusinessRuleException.class);
        value.start(OPENED.plusMinutes(1));
        value.resolve("resolved", null, UUID.randomUUID(), OPENED.plusHours(2));
        value.close(UUID.randomUUID(), true, OPENED.plusHours(3));
        value.reopen("Issue recurred", OPENED.plusHours(4));
        assertThat(value.status()).isEqualTo(OperationalExceptionCase.Status.IN_PROGRESS);
        assertThat(value.resolutionDueAt()).isEqualTo(OPENED.plusHours(28));
    }

    @Test
    void criticalIntakeIsAssignedAndImmediatelyEscalated() {
        var value = caseWith(OperationalExceptionCase.Severity.CRITICAL);
        assertThat(value.assignmentType()).isEqualTo(OperationalExceptionCase.AssignmentType.ROLE_QUEUE);
        assertThat(value.escalationLevel()).isEqualTo(OperationalExceptionCase.EscalationLevel.L1);
        assertThat(value.responseDueAt()).isEqualTo(OPENED.plusMinutes(15));
    }

    private OperationalExceptionCase caseWith(OperationalExceptionCase.Severity severity) {
        return OperationalExceptionCase.open(UUID.randomUUID(), UUID.randomUUID(), "OEX-0123456789AB",
            UUID.randomUUID(), OperationalExceptionCase.SourceModule.ROUTING, "ACCIDENT", UUID.randomUUID(),
            OPENED, "ROUTE_DISRUPTION_CREATED", null, OperationalExceptionCase.Category.SAFETY, severity,
            "OPERATIONS_SAFETY_QUEUE", OPENED);
    }
}
