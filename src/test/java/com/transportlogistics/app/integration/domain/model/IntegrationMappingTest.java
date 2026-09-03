package com.transportlogistics.app.integration.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntegrationMappingTest {
    @Test
    void registeredProbeMappingIsDeterministicAndRejectsScriptsOrUnknownFields() {
        var mapping = mapping(List.of(
            rule("probeId", "probe_id", IntegrationMapping.Format.UUID),
            rule("probeType", "probe_type", IntegrationMapping.Format.ENUM),
            rule("sequence", "sequence", IntegrationMapping.Format.DECIMAL)));

        UUID probeId = UUID.randomUUID();
        assertThat(mapping.apply(Map.of("probeId", probeId.toString(), "probeType", "CONTROLLED_SANDBOX",
            "sequence", 7L))).containsEntry("probe_id", probeId.toString())
            .containsEntry("probe_type", "CONTROLLED_SANDBOX");

        assertThatThrownBy(() -> mapping.apply(Map.of("probeId", probeId.toString(), "probeType",
            "CONTROLLED_SANDBOX", "sequence", 7L, "script", "evil")))
            .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void allThreeRegisteredTargetsAreMandatoryAndUnique() {
        assertThatThrownBy(() -> mapping(List.of(
            rule("probeId", "probe_id", IntegrationMapping.Format.UUID),
            rule("probeType", "probe_type", IntegrationMapping.Format.ENUM))))
            .isInstanceOf(BusinessRuleException.class)
            .extracting("code").isEqualTo("INTEGRATION_MAPPING_INVALID");
    }

    private IntegrationMapping mapping(List<IntegrationMapping.Rule> rules) {
        return IntegrationMapping.active(UUID.randomUUID(), UUID.randomUUID(), "US73_PLATFORM_PROBE", 1,
            IntegrationMapping.PROBE_CONTRACT, 1, IntegrationMapping.PROBE_SCHEMA, 1, rules,
            OffsetDateTime.now(), "operator");
    }

    private IntegrationMapping.Rule rule(String source, String target, IntegrationMapping.Format format) {
        return new IntegrationMapping.Rule(source, target, null, format, false, true);
    }
}
