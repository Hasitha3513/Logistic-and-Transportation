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

    @Test
    void billingContractUsesCanonicalAllowListedPassthroughOnly() {
        var mapping = IntegrationMapping.active(UUID.randomUUID(), UUID.randomUUID(), "TRANSPORT_BILLING_V1", 1,
            IntegrationMapping.BILLING_CONTRACT, 1, IntegrationMapping.BILLING_CONTRACT, 1,
            List.of(rule("billingRecordId", "billingRecordId", IntegrationMapping.Format.UUID)),
            OffsetDateTime.now(), "operator");
        var payload = new java.util.LinkedHashMap<String, Object>();
        payload.put("schemaVersion", 1); payload.put("billingRecordId", UUID.randomUUID().toString());
        payload.put("billingNumber", "TB-2026-000001"); payload.put("recordType", "REGULAR");
        payload.put("customerId", UUID.randomUUID().toString()); payload.put("currency", "LKR");
        payload.put("source", Map.of()); payload.put("amounts", Map.of()); payload.put("tax", Map.of());
        payload.put("costCentres", List.of()); payload.put("originalBillingRecordId", null);
        payload.put("finalizedAt", OffsetDateTime.now().toString());
        assertThat(mapping.apply(payload)).isEqualTo(payload);
        payload.put("bankAccount", "forbidden");
        assertThatThrownBy(() -> mapping.apply(payload)).isInstanceOf(BusinessRuleException.class);
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
