package com.transportlogistics.app.notification.infrastructure.adapters.in.event;

import com.transportlogistics.app.notification.OperationalNotificationEvent;
import com.transportlogistics.app.notification.application.service.NotificationRuleEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class OperationalNotificationEventListener {
    private static final Logger log = LoggerFactory.getLogger(OperationalNotificationEventListener.class);

    private final NotificationRuleEngine ruleEngine;

    public OperationalNotificationEventListener(NotificationRuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    @EventListener
    public void onOperationalEvent(OperationalNotificationEvent event) {
        log.debug("Received OperationalNotificationEvent in listener: {}", event);
        try {
            ruleEngine.processEvent(event);
        } catch (Exception e) {
            log.error("Error processing OperationalNotificationEvent {}: {}", event.eventId(), e.getMessage(), e);
        }
    }
}
