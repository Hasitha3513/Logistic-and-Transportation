package com.transportlogistics.app.tracking.adapters.inbound.flespi;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component("flespiAdapterProperties")
@ConfigurationProperties(prefix = "app.tracking.flespi")
public final class FlespiAdapterProperties {
    static final Duration MINIMUM_POLL_INTERVAL = Duration.ofSeconds(5);
    static final Duration MAXIMUM_OVERLAP = Duration.ofMinutes(5);
    static final int MAXIMUM_PAGE_SIZE = 500;
    static final int MAXIMUM_RESPONSE_BYTES = 1_048_576;

    private boolean enabled;
    private URI baseUrl = URI.create("https://flespi.io");
    private URI trackingIngressUrl = URI.create("http://localhost:8080/api/integration/v1/tracking/positions");
    private String providerKeyId;
    private String providerAlias;
    private long flespiDeviceId = -1;
    private String deviceIdent;
    private UUID trackingDeviceId;
    private Duration pollInterval = MINIMUM_POLL_INTERVAL;
    private Duration connectTimeout = Duration.ofSeconds(3);
    private Duration requestTimeout = Duration.ofSeconds(10);
    private int pageSize = MAXIMUM_PAGE_SIZE;
    private Duration overlapWindow = MAXIMUM_OVERLAP;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public URI getBaseUrl() { return baseUrl; }
    public void setBaseUrl(URI baseUrl) { this.baseUrl = baseUrl; }
    public URI getTrackingIngressUrl() { return trackingIngressUrl; }
    public void setTrackingIngressUrl(URI trackingIngressUrl) { this.trackingIngressUrl = trackingIngressUrl; }
    public String getProviderKeyId() { return providerKeyId; }
    public void setProviderKeyId(String providerKeyId) { this.providerKeyId = providerKeyId; }
    public String getProviderAlias() { return providerAlias; }
    public void setProviderAlias(String providerAlias) { this.providerAlias = providerAlias; }
    public long getFlespiDeviceId() { return flespiDeviceId; }
    public void setFlespiDeviceId(long flespiDeviceId) { this.flespiDeviceId = flespiDeviceId; }
    public String getDeviceIdent() { return deviceIdent; }
    public void setDeviceIdent(String deviceIdent) { this.deviceIdent = deviceIdent; }
    public UUID getTrackingDeviceId() { return trackingDeviceId; }
    public void setTrackingDeviceId(UUID trackingDeviceId) { this.trackingDeviceId = trackingDeviceId; }
    public Duration getPollInterval() { return normalizedPollInterval(); }
    public void setPollInterval(Duration pollInterval) { this.pollInterval = pollInterval; }
    public Duration getConnectTimeout() { return positive(connectTimeout, Duration.ofSeconds(3)); }
    public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
    public Duration getRequestTimeout() { return positive(requestTimeout, Duration.ofSeconds(10)); }
    public void setRequestTimeout(Duration requestTimeout) { this.requestTimeout = requestTimeout; }
    public int getPageSize() { return Math.max(1, Math.min(pageSize, MAXIMUM_PAGE_SIZE)); }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public Duration getOverlapWindow() {
        if (overlapWindow == null || overlapWindow.isNegative()) return MAXIMUM_OVERLAP;
        return overlapWindow.compareTo(MAXIMUM_OVERLAP) > 0 ? MAXIMUM_OVERLAP : overlapWindow;
    }
    public void setOverlapWindow(Duration overlapWindow) { this.overlapWindow = overlapWindow; }

    long pollIntervalMillis() { return normalizedPollInterval().toMillis(); }

    boolean configured() {
        return enabled && providerKeyId != null && !providerKeyId.isBlank()
                && providerAlias != null && !providerAlias.isBlank()
                && flespiDeviceId >= 0 && deviceIdent != null && !deviceIdent.isBlank()
                && trackingDeviceId != null && validFlespiUrl() && validPrivateIngressUrl();
    }

    private Duration normalizedPollInterval() {
        return pollInterval == null || pollInterval.compareTo(MINIMUM_POLL_INTERVAL) < 0
                ? MINIMUM_POLL_INTERVAL : pollInterval;
    }

    private boolean validFlespiUrl() {
        return baseUrl != null && "https".equalsIgnoreCase(baseUrl.getScheme()) && baseUrl.getHost() != null;
    }

    private boolean validPrivateIngressUrl() {
        if (trackingIngressUrl == null || trackingIngressUrl.getHost() == null) return false;
        try {
            boolean local = java.net.InetAddress.getByName(trackingIngressUrl.getHost()).isLoopbackAddress();
            return local && ("http".equalsIgnoreCase(trackingIngressUrl.getScheme())
                    || "https".equalsIgnoreCase(trackingIngressUrl.getScheme()));
        } catch (java.net.UnknownHostException exception) {
            return false;
        }
    }

    private static Duration positive(Duration candidate, Duration fallback) {
        return candidate == null || candidate.isZero() || candidate.isNegative() ? fallback : candidate;
    }
}
