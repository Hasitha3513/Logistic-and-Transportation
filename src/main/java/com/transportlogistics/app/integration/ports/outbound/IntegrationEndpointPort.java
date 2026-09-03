package com.transportlogistics.app.integration.ports.outbound;

import java.util.UUID;

public interface IntegrationEndpointPort {
    void probe(String endpointAlias, UUID probeId);
    DeliveryResult deliver(String endpointAlias, UUID exchangeId, String canonicalPayload, String payloadHash);

    record DeliveryResult(String externalCorrelationId, String targetFilename) {}

    final class EndpointFailure extends RuntimeException {
        private final String code;
        private final boolean retryable;

        public EndpointFailure(String code, String message, boolean retryable) {
            super(message);
            this.code = code;
            this.retryable = retryable;
        }

        public EndpointFailure(String code, String message, boolean retryable, Throwable cause) {
            super(message, cause);
            this.code = code;
            this.retryable = retryable;
        }

        public String code() { return code; }
        public boolean retryable() { return retryable; }
    }
}
