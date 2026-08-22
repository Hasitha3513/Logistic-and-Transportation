package com.transportlogistics.app.notification.infrastructure.adapters.in.web.controllers;

import com.transportlogistics.app.notification.application.service.NotificationEmailDeliveryWorker;
import com.transportlogistics.app.notification.infrastructure.testing.E2eAdjustableClock;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;

@RestController
@Profile("e2e")
@RequestMapping("/e2e/notifications")
public class E2eNotificationTestController {
    private final E2eAdjustableClock clock;
    private final NotificationEmailDeliveryWorker worker;

    public E2eNotificationTestController(E2eAdjustableClock clock, NotificationEmailDeliveryWorker worker) {
        this.clock = clock;
        this.worker = worker;
    }

    @GetMapping("/now")
    TimeResponse now() {
        return new TimeResponse(clock.instant());
    }

    @PostMapping("/advance")
    TimeResponse advance(@RequestParam Duration duration) {
        clock.advance(duration);
        return now();
    }

    @PostMapping("/reset")
    TimeResponse reset() {
        clock.reset();
        return now();
    }

    @PostMapping("/process-email")
    ResponseEntity<Void> processEmail() {
        worker.processDue();
        return ResponseEntity.noContent().build();
    }

    record TimeResponse(Instant now) {}
}
