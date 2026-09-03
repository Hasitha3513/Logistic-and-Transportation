package com.transportlogistics.app.notification.infrastructure.adapters.in.scheduling;

import com.transportlogistics.app.notification.application.service.NotificationEmailDeliveryWorker;
import com.transportlogistics.app.tenancy.TenantJobExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationEmailDeliveryScheduler {
    private final NotificationEmailDeliveryWorker worker;
    private final TenantJobExecutor tenants;

    public NotificationEmailDeliveryScheduler(NotificationEmailDeliveryWorker worker, TenantJobExecutor tenants) {
        this.worker = worker;
        this.tenants = tenants;
    }

    @Scheduled(fixedDelayString = "${app.notification.email.worker-delay:PT30S}")
    public void processDueEmail() { tenants.forEachActiveTenant("notification-email-delivery", worker::processDue); }
}
