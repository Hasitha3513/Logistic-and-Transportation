package com.transportlogistics.app.notification.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationTemplateRendererTest {
    private final NotificationTemplateRenderer renderer = new NotificationTemplateRenderer();

    @Test
    void validatesPositiveVersionAndKnownVariables() {
        assertThatThrownBy(() -> template("{{severity}}", "{{eventTime}}", 0))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("positive");
        assertThatThrownBy(() -> template("{{unknown}}", "{{eventTime}}", 1))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unknown");
        assertThatThrownBy(() -> template("{{severity", "{{eventTime}}", 1))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Malformed");
    }

    @Test
    void rendersRequiredAndOptionalVariablesAsPlainText() {
        var rendered = renderer.render(
            template("Trip {{tripNumber}} delayed", "{{reason}} {{locationDescription}}", 1), requiredVariables());
        assertThat(rendered.subject()).isEqualTo("Trip TRIP-001 delayed");
        assertThat(rendered.body()).isEqualTo("Traffic ");
    }

    @Test
    void missingRequiredVariableFailsWithStableCode() {
        Map<String, String> variables = new HashMap<>(requiredVariables());
        variables.remove("delayMinutes");

        assertThatThrownBy(() -> renderer.render(template("{{severity}}", "{{eventTime}}", 1), variables))
            .isInstanceOfSatisfying(BusinessRuleException.class,
                error -> assertThat(error.code()).isEqualTo("TEMPLATE_DATA_MISSING"));
    }

    @Test
    void rejectsUnsafeControlsAndRenderedLengthOverflow() {
        assertThatThrownBy(() -> template("Bad\u0001", "{{eventTime}}", 1))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("unsafe control");
        Map<String, String> variables = new HashMap<>(requiredVariables());
        variables.put("reason", "x".repeat(4000));
        assertThatThrownBy(() -> renderer.render(template("{{severity}}", "{{reason}} {{eventTime}}", 1), variables))
            .isInstanceOfSatisfying(BusinessRuleException.class,
                error -> assertThat(error.code()).isEqualTo("NOTIFICATION_TEMPLATE_INVALID"));
    }

    @Test
    void normalizesLineEndings() {
        var rendered = renderer.render(template("{{severity}}", "First\r\n{{eventTime}}", 1), requiredVariables());
        assertThat(rendered.body()).doesNotContain("\r").contains("\n");
    }

    private NotificationTemplate template(String subject, String body, int version) {
        var now = OffsetDateTime.now();
        return new NotificationTemplate(UUID.randomUUID(), "TRIP_DELAY", "Trip delay", "TRIP_DELAY_RECORDED",
            NotificationChannel.IN_APP, subject, body, version, true, now, now);
    }

    private Map<String, String> requiredVariables() {
        return Map.of(
            "eventTime", "2026-08-21T12:00:00Z", "severity", "WARNING",
            "tripId", UUID.randomUUID().toString(), "tripNumber", "TRIP-001",
            "delayMinutes", "25", "reason", "Traffic");
    }
}
