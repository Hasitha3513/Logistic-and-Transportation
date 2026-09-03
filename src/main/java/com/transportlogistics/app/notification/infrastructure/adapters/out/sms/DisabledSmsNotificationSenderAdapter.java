package com.transportlogistics.app.notification.infrastructure.adapters.out.sms;

import com.transportlogistics.app.notification.application.ports.out.SmsNotificationSenderPort;
import com.transportlogistics.app.notification.domain.model.EmailDeliveryErrorCategory;

/** Fail-closed SMS sender used when no real production provider is configured. */
public class DisabledSmsNotificationSenderAdapter implements SmsNotificationSenderPort {
    @Override
    public SendResult send(SendRequest request) {
        return SendResult.rejected(EmailDeliveryErrorCategory.CONFIGURATION,
            "SMS_DISABLED", "SMS delivery is disabled");
    }
}
