package com.transportlogistics.app.notification.infrastructure.config;

import com.transportlogistics.app.notification.application.ports.out.NotificationDeliveryPort;
import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationTemplateRenderer;
import com.transportlogistics.app.notification.domain.model.NotificationQuietHoursEvaluator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.EnumMap;
import java.util.Map;
import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableScheduling
public class NotificationConfig {

    @Bean
    public NotificationTemplateRenderer notificationTemplateRenderer() {
        return new NotificationTemplateRenderer();
    }

    @Bean
    public NotificationQuietHoursEvaluator notificationQuietHoursEvaluator(
        Clock clock,
        @Value("${app.notification.time-zone:UTC}") String timeZone
    ) {
        return new NotificationQuietHoursEvaluator(clock, ZoneId.of(timeZone));
    }

    @Bean
    public Map<NotificationChannel, NotificationDeliveryPort> notificationDeliveryPorts(
        @Qualifier("inAppNotificationDeliveryAdapter") NotificationDeliveryPort inAppAdapter
    ) {
        Map<NotificationChannel, NotificationDeliveryPort> map = new EnumMap<>(NotificationChannel.class);
        map.put(NotificationChannel.IN_APP, inAppAdapter);
        return map;
    }
}
