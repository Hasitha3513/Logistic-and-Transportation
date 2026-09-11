package com.transportlogistics.app.integration.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntegrationConfigurationTest {
    private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 9, 3, 12, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void onlyFrozenCapabilityIsAccepted() {
        assertThatThrownBy(() -> IntegrationConfiguration.draft(UUID.randomUUID(), "Test",
            IntegrationConfiguration.Type.FILE_EXCHANGE, IntegrationConfiguration.Protocol.FILE_JSON_V1,
            IntegrationConfiguration.Direction.INBOUND, "CONTROLLED_SANDBOX", null,
            IntegrationConfiguration.DataClassification.INTERNAL_OPERATIONAL_NON_SENSITIVE, NOW, "operator"))
            .isInstanceOf(BusinessRuleException.class)
            .extracting("code").isEqualTo("INTEGRATION_CAPABILITY_UNSUPPORTED");

        assertThatThrownBy(() -> IntegrationConfiguration.draft(UUID.randomUUID(), "Test",
            IntegrationConfiguration.Type.FILE_EXCHANGE, IntegrationConfiguration.Protocol.FILE_JSON_V1,
            IntegrationConfiguration.Direction.OUTBOUND, "CONTROLLED_SANDBOX", null,
            IntegrationConfiguration.DataClassification.FINANCIAL, NOW, "operator"))
            .isInstanceOf(BusinessRuleException.class)
            .extracting("code").isEqualTo("INTEGRATION_CAPABILITY_UNSUPPORTED");
    }

    @Test
    void activationRequiresCurrentVersionTestWithinFifteenMinutes() {
        UUID tenant = UUID.randomUUID();
        UUID mapping = UUID.randomUUID();
        var stale = configuration(tenant, mapping, NOW.minusMinutes(16), 3L, 3);
        assertThatThrownBy(() -> stale.activate(NOW, "operator"))
            .isInstanceOf(BusinessRuleException.class)
            .extracting("code").isEqualTo("INTEGRATION_CONFIGURATION_INVALID");

        var fresh = configuration(tenant, mapping, NOW.minusMinutes(14), 3L, 3);
        assertThat(fresh.activate(NOW, "operator").lifecycle()).isEqualTo(IntegrationConfiguration.Lifecycle.ACTIVE);
    }

    @Test
    void providerFailureChangesHealthWithoutChangingLifecycle() {
        var active = configuration(UUID.randomUUID(), UUID.randomUUID(), NOW.minusMinutes(1), 3L, 3)
            .activate(NOW, "operator");
        var degraded = active.exchangeFailed(true, false, NOW.plusMinutes(1));
        assertThat(degraded.lifecycle()).isEqualTo(IntegrationConfiguration.Lifecycle.ACTIVE);
        assertThat(degraded.health()).isEqualTo(IntegrationConfiguration.Health.DEGRADED);
    }

    @Test
    void activeConfigurationRejectsMaterialAndMappingEdits() {
        var active = configuration(UUID.randomUUID(), UUID.randomUUID(), NOW.minusMinutes(1), 3L, 3)
            .activate(NOW, "operator");

        assertThatThrownBy(() -> active.update("Changed", "CONTROLLED_SANDBOX", null, UUID.randomUUID(),
            NOW.plusMinutes(1), "operator"))
            .isInstanceOf(BusinessRuleException.class)
            .extracting("code").isEqualTo("INTEGRATION_CONFIGURATION_INVALID");
        assertThatThrownBy(() -> active.withMapping(UUID.randomUUID(), NOW.plusMinutes(1), "operator"))
            .isInstanceOf(BusinessRuleException.class)
            .extracting("code").isEqualTo("INTEGRATION_CONFIGURATION_INVALID");
    }

    private IntegrationConfiguration configuration(UUID tenant, UUID mapping, OffsetDateTime testedAt,
                                                   Long testedVersion, long version) {
        return new IntegrationConfiguration(UUID.randomUUID(), tenant, "Test", "TEST",
            IntegrationConfiguration.Type.FILE_EXCHANGE, IntegrationConfiguration.Protocol.FILE_JSON_V1,
            IntegrationConfiguration.Direction.OUTBOUND, "CONTROLLED_SANDBOX", null, mapping,
            IntegrationConfiguration.DataClassification.INTERNAL_OPERATIONAL_NON_SENSITIVE,
            IntegrationConfiguration.RetryPolicy.US73_BOUNDED_V1, IntegrationConfiguration.Lifecycle.DRAFT,
            IntegrationConfiguration.Health.HEALTHY, testedAt, testedVersion, null, version, NOW.minusHours(1),
            "operator", NOW.minusHours(1), "operator");
    }
}
