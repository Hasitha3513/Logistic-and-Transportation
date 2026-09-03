package com.transportlogistics.app.integration.ports.outbound;

import java.util.Map;

public interface IntegrationPayloadPort {
    String serialize(Map<String, ?> payload);
    String hash(String payload);
}
