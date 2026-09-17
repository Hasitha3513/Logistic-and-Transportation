package com.transportlogistics.app.tracking.adapters.inbound.traccar;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
final class TraccarEndpointPolicy {
    private final Set<String> privateAllowlist;
    private final AddressResolver resolver;

    @Autowired
    TraccarEndpointPolicy(
            @Value("${app.tracking.traccar.private-endpoint-allowlist:}") String allowlist) {
        this(parse(allowlist), InetAddress::getAllByName);
    }

    TraccarEndpointPolicy(Set<String> privateAllowlist, AddressResolver resolver) {
        this.privateAllowlist = Set.copyOf(privateAllowlist);
        this.resolver = resolver;
    }

    boolean structurallyAllowed(URI endpoint) {
        if (endpoint == null || !"https".equalsIgnoreCase(endpoint.getScheme())
                || endpoint.getHost() == null || endpoint.getUserInfo() != null
                || endpoint.getRawQuery() != null || endpoint.getFragment() != null
                || endpoint.getPath() != null && !endpoint.getPath().isBlank()
                && !"/".equals(endpoint.getPath())) {
            return false;
        }
        int port = effectivePort(endpoint);
        return port == 443 || privateAllowlist.contains(authority(endpoint));
    }

    void authorizeConnection(URI endpoint) {
        if (!structurallyAllowed(endpoint)) {
            throw blocked();
        }
        boolean privateApproved = privateAllowlist.contains(authority(endpoint));
        try {
            InetAddress[] addresses = resolver.resolve(endpoint.getHost());
            if (addresses.length == 0 || Arrays.stream(addresses)
                    .anyMatch(address -> forbidden(address, privateApproved))) {
                throw blocked();
            }
        } catch (UnknownHostException exception) {
            throw new TraccarFailure(
                    TraccarFailure.Kind.TRANSIENT, "provider_dns_unavailable", exception);
        }
    }

    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    private static boolean forbidden(InetAddress address, boolean privateApproved) {
        byte[] raw = address.getAddress();
        boolean uniqueLocalV6 = raw.length == 16 && (raw[0] & 0xfe) == 0xfc;
        boolean metadataV6 = address.getHostAddress().equalsIgnoreCase("fd00:ec2::254");
        if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                || address.isLinkLocalAddress() || address.isMulticastAddress() || metadataV6) {
            return true;
        }
        return !privateApproved && (address.isSiteLocalAddress() || uniqueLocalV6);
    }

    private static String authority(URI endpoint) {
        return endpoint.getHost().toLowerCase(Locale.ROOT) + ":" + effectivePort(endpoint);
    }

    private static int effectivePort(URI endpoint) {
        return endpoint.getPort() == -1 ? 443 : endpoint.getPort();
    }

    private static Set<String> parse(String configured) {
        if (configured == null || configured.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(configured.split(","))
                .map(String::trim).filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static TraccarFailure blocked() {
        return new TraccarFailure(
                TraccarFailure.Kind.PERMANENT, "provider_endpoint_blocked", null);
    }

    @FunctionalInterface
    interface AddressResolver {
        InetAddress[] resolve(String host) throws UnknownHostException;
    }
}
