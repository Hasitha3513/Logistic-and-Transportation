package com.transportlogistics.app.notification.infrastructure.adapters.out.event;

import com.transportlogistics.app.notification.OperationalNotificationEvent;
import com.transportlogistics.app.notification.OperationalNotificationPublisher;
import com.transportlogistics.app.shared.AfterCommitEventPublisher;
import com.transportlogistics.app.tenancy.CurrentTenant;
import org.springframework.stereotype.Component;

@Component
public final class SpringOperationalNotificationPublisher implements OperationalNotificationPublisher {
    private final AfterCommitEventPublisher events;
    private final CurrentTenant currentTenant;

    public SpringOperationalNotificationPublisher(AfterCommitEventPublisher events, CurrentTenant currentTenant) {
        this.events = events;
        this.currentTenant = currentTenant;
    }

    @Override
    public void publish(OperationalNotificationEvent event) {
        events.publish(event.tenantId() == null
                ? event.withTenantId(currentTenant.required().tenantId())
                : event);
    }
}
