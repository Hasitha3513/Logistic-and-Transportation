package com.transportlogistics.app.notification.infrastructure.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationEmailPropertiesTest {
    @Test void validProductionSmtpConfigurationPasses() { production().validate(true); }

    @Test void disabledTestConfigurationPassesWithoutProviderCredentials() {
        NotificationEmailProperties properties = test(false);
        properties.validate(false);
    }

    @Test void testModeCannotRunWithProductionProfile() {
        assertThatThrownBy(() -> test(true).validate(true)).hasMessageContaining("test mode is forbidden");
    }

    @Test void enabledProductionRequiresSenderHostAndCredentials() {
        NotificationEmailProperties missingFrom = production();
        missingFrom.setFrom("");
        assertThatThrownBy(() -> missingFrom.validate(true)).hasMessageContaining("from address is required");

        NotificationEmailProperties missingHost = production();
        missingHost.getSmtp().setHost("");
        assertThatThrownBy(() -> missingHost.validate(true)).hasMessageContaining("smtp.host");

        NotificationEmailProperties missingPassword = production();
        missingPassword.getSmtp().setPassword("");
        assertThatThrownBy(() -> missingPassword.validate(true)).hasMessageContaining("credentials");
    }

    @Test void rejectsInvalidModeProviderTlsPortAndTimeout() {
        NotificationEmailProperties invalidMode = production(); invalidMode.setMode("staging");
        assertThatThrownBy(() -> invalidMode.validate(false)).hasMessageContaining("mode");
        NotificationEmailProperties invalidProvider = production(); invalidProvider.setProvider("vendor");
        assertThatThrownBy(() -> invalidProvider.validate(false)).hasMessageContaining("provider 'smtp'");
        NotificationEmailProperties invalidTls = production(); invalidTls.getSmtp().setTlsMode("optional");
        assertThatThrownBy(() -> invalidTls.validate(false)).hasMessageContaining("tls-mode");
        NotificationEmailProperties invalidPort = production(); invalidPort.getSmtp().setPort(0);
        assertThatThrownBy(() -> invalidPort.validate(false)).hasMessageContaining("smtp.port");
        NotificationEmailProperties invalidTimeout = production(); invalidTimeout.setReadTimeout(Duration.ofHours(1));
        assertThatThrownBy(() -> invalidTimeout.validate(false)).hasMessageContaining("read-timeout");
    }

    @Test void plaintextSmtpIsLoopbackOnlyAndSecretsAreRedacted() {
        NotificationEmailProperties properties = production();
        properties.getSmtp().setTlsMode("none");
        assertThatThrownBy(() -> properties.validate(false)).hasMessageContaining("loopback");
        assertThat(properties.toString()).doesNotContain("smtp-secret").contains("<redacted>");
    }

    static NotificationEmailProperties production() {
        NotificationEmailProperties p = common();
        p.setEnabled(true); p.setMode("production"); p.setProvider("smtp"); p.setFrom("noreply@example.test");
        p.getSmtp().setHost("smtp.example.test"); p.getSmtp().setPort(587); p.getSmtp().setTlsMode("starttls");
        p.getSmtp().setAuthenticationRequired(true); p.getSmtp().setUsername("smtp-user"); p.getSmtp().setPassword("smtp-secret");
        return p;
    }

    static NotificationEmailProperties test(boolean enabled) {
        NotificationEmailProperties p = common();
        p.setEnabled(enabled); p.setMode("test"); p.setProvider("test"); p.setFrom("noreply@example.test");
        return p;
    }

    static NotificationEmailProperties common() {
        NotificationEmailProperties p = new NotificationEmailProperties();
        p.setConnectTimeout(Duration.ofSeconds(1)); p.setReadTimeout(Duration.ofSeconds(1));
        return p;
    }
}
