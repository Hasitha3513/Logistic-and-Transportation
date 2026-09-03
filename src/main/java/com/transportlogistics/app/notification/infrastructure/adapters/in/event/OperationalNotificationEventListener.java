package com.transportlogistics.app.notification.infrastructure.adapters.in.event;

import com.transportlogistics.app.notification.OperationalNotificationEvent;
import com.transportlogistics.app.notification.application.service.NotificationRuleEngine;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class OperationalNotificationEventListener {

    private final NotificationRuleEngine ruleEngine;

    public OperationalNotificationEventListener(NotificationRuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    @EventListener
    public void onOperationalEvent(OperationalNotificationEvent event) {
        ruleEngine.processEvent(event);
    }
}
