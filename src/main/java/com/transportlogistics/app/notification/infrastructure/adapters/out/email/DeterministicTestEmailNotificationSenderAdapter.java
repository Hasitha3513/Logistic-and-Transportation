package com.transportlogistics.app.notification.infrastructure.adapters.out.email;

import com.transportlogistics.app.notification.application.ports.out.EmailNotificationSenderPort;
import com.transportlogistics.app.notification.domain.model.EmailDeliveryErrorCategory;

/** Safe test-mode sender. It never contacts the internet and never reports provider acceptance. */
public class DeterministicTestEmailNotificationSenderAdapter implements EmailNotificationSenderPort {
    @Override
    public SendResult send(SendRequest request) {
        return SendResult.rejected(EmailDeliveryErrorCategory.CONFIGURATION,
            "EMAIL_TEST_MODE", "EMAIL test mode does not represent provider acceptance");
    }
}
