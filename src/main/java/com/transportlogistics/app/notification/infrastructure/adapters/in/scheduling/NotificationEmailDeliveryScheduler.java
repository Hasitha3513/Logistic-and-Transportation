package com.transportlogistics.app.notification.infrastructure.adapters.in.scheduling;

import com.transportlogistics.app.notification.application.service.NotificationEmailDeliveryWorker;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationEmailDeliveryScheduler {
    private final NotificationEmailDeliveryWorker worker;

    public NotificationEmailDeliveryScheduler(NotificationEmailDeliveryWorker worker) { this.worker = worker; }

    @Scheduled(fixedDelayString = "${app.notification.email.worker-delay:PT30S}")
    public void processDueEmail() { worker.processDue(); }
}
