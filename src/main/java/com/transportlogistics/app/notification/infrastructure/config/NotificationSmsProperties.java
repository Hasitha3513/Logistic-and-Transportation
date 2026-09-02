package com.transportlogistics.app.notification.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.notification.sms")
public class NotificationSmsProperties {
    private boolean enabled;
    private String mode = "disabled";
    private Duration readTimeout = Duration.ofSeconds(30);

    public void validate(boolean productionProfile) {
        if (readTimeout == null || readTimeout.isNegative() || readTimeout.isZero()
            || readTimeout.compareTo(Duration.ofMinutes(2)) > 0) {
            throw new IllegalStateException("Invalid notification SMS configuration: read-timeout is invalid");
        }
        if (enabled && !"test".equalsIgnoreCase(mode)) {
            throw new IllegalStateException("Invalid notification SMS configuration: no production provider configured");
        }
        if (productionProfile && enabled) {
            throw new IllegalStateException("Invalid notification SMS configuration: test adapter is forbidden in production");
        }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }
}
