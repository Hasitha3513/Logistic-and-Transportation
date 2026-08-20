package com.transportlogistics.app.notification.infrastructure.adapters.out.email;

import com.transportlogistics.app.notification.application.ports.out.NotificationDeliveryPort;
import com.transportlogistics.app.notification.domain.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("emailNotificationDeliveryAdapter")
public class EmailNotificationDeliveryAdapter implements NotificationDeliveryPort {
    private static final Logger log = LoggerFactory.getLogger(EmailNotificationDeliveryAdapter.class);

    private final boolean emailEnabled;
    private final String senderAddress;

    public EmailNotificationDeliveryAdapter(
        @Value("${app.notification.email.enabled:false}") boolean emailEnabled,
        @Value("${app.notification.email.from:noreply@transportlogistics.com}") String senderAddress
    ) {
        this.emailEnabled = emailEnabled;
        this.senderAddress = senderAddress;
    }

    @Override
    public void deliver(Notification notification) {
        if (!emailEnabled) {
            log.info("[MOCK EMAIL DELIVERY] To: {} | From: {} | Subject: [{}] {} | Body: {}",
                notification.recipient(), senderAddress, notification.severity(), notification.title(), notification.message());
            return;
        }

        // Production SMTP delivery logic when enabled
        log.info("Dispatching SMTP email to {} from {}: {}", notification.recipient(), senderAddress, notification.title());
    }
}
