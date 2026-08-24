package com.transportlogistics.app.notification.infrastructure.adapters.out.email;

import com.transportlogistics.app.notification.application.ports.out.EmailNotificationSenderPort;
import com.transportlogistics.app.notification.domain.model.EmailDeliveryErrorCategory;

/** Explicit disabled sender selected when EMAIL delivery is disabled. */
public class EmailNotificationDeliveryAdapter implements EmailNotificationSenderPort {

    @Override
    public SendResult send(SendRequest request) {
        return SendResult.rejected(EmailDeliveryErrorCategory.CONFIGURATION,
            "EMAIL_DISABLED", "EMAIL delivery is disabled");
    }
}
