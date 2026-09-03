package com.transportlogistics.app.notification.infrastructure.config;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Locale;

@ConfigurationProperties("app.notification.email")
public class NotificationEmailProperties {
    static final Duration MIN_TIMEOUT = Duration.ofMillis(100);
    static final Duration MAX_CONNECT_TIMEOUT = Duration.ofMinutes(1);
    static final Duration MAX_READ_TIMEOUT = Duration.ofMinutes(2);

    private boolean enabled;
    private String mode;
    private String provider;
    private String from;
    private String replyTo;
    private Duration connectTimeout;
    private Duration readTimeout;
    private Smtp smtp = new Smtp();

    public void validate(boolean productionProfile) {
        String normalizedMode = normalized(mode);
        if (!"production".equals(normalizedMode) && !"test".equals(normalizedMode)) {
            throw invalid("mode must be exactly 'production' or 'test'");
        }
        if (productionProfile && "test".equals(normalizedMode)) {
            throw invalid("test mode is forbidden with a production database profile");
        }

        String normalizedProvider = normalized(provider);
        if ("production".equals(normalizedMode) && !"smtp".equals(normalizedProvider)) {
            throw invalid("production mode requires provider 'smtp'");
        }
        if ("test".equals(normalizedMode) && !"test".equals(normalizedProvider)) {
            throw invalid("test mode requires provider 'test'");
        }

        requireDuration(connectTimeout, MIN_TIMEOUT, MAX_CONNECT_TIMEOUT, "connect-timeout");
        requireDuration(readTimeout, MIN_TIMEOUT, MAX_READ_TIMEOUT, "read-timeout");

        if (!enabled) return;
        requireAddress(from, "from");
        if (replyTo != null && !replyTo.isBlank()) requireAddress(replyTo, "reply-to");
        if ("production".equals(normalizedMode)) validateSmtp();
    }

    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    private void validateSmtp() {
        if (smtp == null) throw invalid("smtp configuration is required");
        String host = normalized(smtp.host);
        if (host == null || (!"::1".equals(host) && !host.matches("[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?"))) {
            throw invalid("smtp.host is missing or invalid");
        }
        if (smtp.port < 1 || smtp.port > 65535) throw invalid("smtp.port must be between 1 and 65535");
        String tlsMode = normalized(smtp.tlsMode);
        if (!"starttls".equals(tlsMode) && !"ssl".equals(tlsMode) && !"none".equals(tlsMode)) {
            throw invalid("smtp.tls-mode must be 'starttls', 'ssl', or 'none'");
        }
        if ("none".equals(tlsMode) && !isLoopback(host)) {
            throw invalid("unencrypted SMTP is permitted only for a loopback integration server");
        }
        if (smtp.authenticationRequired
            && (smtp.username == null || smtp.username.isBlank() || smtp.password == null || smtp.password.isBlank())) {
            throw invalid("smtp credentials are required when authentication-required is true");
        }
    }

    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    private static boolean isLoopback(String host) {
        return "localhost".equals(host) || "127.0.0.1".equals(host) || "::1".equals(host);
    }

    private static void requireAddress(String value, String property) {
        if (value == null || value.isBlank()) throw invalid(property + " address is required");
        try {
            InternetAddress address = new InternetAddress(value.trim(), true);
            address.validate();
        } catch (AddressException exception) {
            throw invalid(property + " address is invalid");
        }
    }

    private static void requireDuration(Duration value, Duration minimum, Duration maximum, String property) {
        if (value == null || value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
            throw invalid(property + " must be between " + minimum + " and " + maximum);
        }
    }

    private static IllegalStateException invalid(String message) {
        return new IllegalStateException("Invalid notification EMAIL configuration: " + message);
    }

    private static String normalized(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    public String getReplyTo() { return replyTo; }
    public void setReplyTo(String replyTo) { this.replyTo = replyTo; }
    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }
    public Smtp getSmtp() { return smtp; }
    public void setSmtp(Smtp smtp) { this.smtp = smtp; }

    public boolean productionMode() { return "production".equals(normalized(mode)); }
    public boolean testMode() { return "test".equals(normalized(mode)); }

    @Override
    public String toString() {
        return "NotificationEmailProperties{enabled=" + enabled + ", mode='" + mode + "', provider='" + provider
            + "', from='" + from + "', replyTo='" + replyTo + "', connectTimeout=" + connectTimeout
            + ", readTimeout=" + readTimeout + ", smtp=" + smtp + "}";
    }

    public static class Smtp {
        private String host;
        private int port;
        private String tlsMode;
        private boolean authenticationRequired;
        private String username;
        private String password;

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getTlsMode() { return tlsMode; }
        public void setTlsMode(String tlsMode) { this.tlsMode = tlsMode; }
        public boolean isAuthenticationRequired() { return authenticationRequired; }
        public void setAuthenticationRequired(boolean authenticationRequired) { this.authenticationRequired = authenticationRequired; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        @Override
        public String toString() {
            return "Smtp{host='" + host + "', port=" + port + ", tlsMode='" + tlsMode
                + "', authenticationRequired=" + authenticationRequired + ", usernameConfigured="
                + (username != null && !username.isBlank()) + ", password=<redacted>}";
        }
    }
}
