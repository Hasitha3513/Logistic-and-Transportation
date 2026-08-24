package com.transportlogistics.app.notification.infrastructure.testing;

import com.transportlogistics.app.notification.application.ports.out.EmailNotificationSenderPort;
import com.transportlogistics.app.notification.domain.model.EmailDeliveryErrorCategory;

/** E2E-only sender. Recipient prefixes select deterministic, internet-free outcomes. */
public final class E2eDeterministicEmailSender implements EmailNotificationSenderPort {
    public static final String TERMINAL_PREFIX = "e2e-terminal-";
    public static final String RETRY_PREFIX = "e2e-retry-";

    @Override
    public SendResult send(SendRequest request) {
        String recipient = request.to().toLowerCase();
        if (recipient.startsWith(RETRY_PREFIX)) {
            if (request.idempotencyKey().endsWith(":1")) {
                return SendResult.rejected(EmailDeliveryErrorCategory.CONNECTION,
                    "E2E_TRANSIENT", "Deterministic transient EMAIL failure");
            }
            return SendResult.accepted("e2e-provider-" + request.idempotencyKey());
        }
        if (recipient.startsWith(TERMINAL_PREFIX)) {
            return SendResult.rejected(EmailDeliveryErrorCategory.INVALID_RECIPIENT,
                "E2E_TERMINAL", "Deterministic terminal EMAIL failure");
        }
        return SendResult.rejected(EmailDeliveryErrorCategory.CONFIGURATION,
            "E2E_UNCONFIGURED", "E2E recipient did not select a deterministic outcome");
    }
}
