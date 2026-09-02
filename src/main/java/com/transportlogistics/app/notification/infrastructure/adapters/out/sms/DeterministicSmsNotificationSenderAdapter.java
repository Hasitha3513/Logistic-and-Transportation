package com.transportlogistics.app.notification.infrastructure.adapters.out.sms;

import com.transportlogistics.app.notification.application.ports.out.SmsNotificationSenderPort;
import com.transportlogistics.app.notification.domain.model.EmailDeliveryErrorCategory;

/** Local-only deterministic SMS sender. It performs no network or logging operations. */
public class DeterministicSmsNotificationSenderAdapter implements SmsNotificationSenderPort {
    @Override
    public SendResult send(SendRequest request) {
        if (request.to().endsWith("0002")) {
            return SendResult.rejected(EmailDeliveryErrorCategory.TIMEOUT,
                "SMS_TEST_RETRYABLE", "Deterministic retryable SMS failure");
        }
        if (request.to().endsWith("0003")) {
            return SendResult.rejected(EmailDeliveryErrorCategory.PROVIDER_4XX,
                "SMS_TEST_PERMANENT", "Deterministic permanent SMS failure");
        }
        return SendResult.accepted("sms-test-" + request.notificationId());
    }
}
