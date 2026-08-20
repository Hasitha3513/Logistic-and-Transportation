package com.transportlogistics.app.notification.infrastructure.config;

import com.transportlogistics.app.notification.application.ports.out.NotificationDeliveryPort;
import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.Map;

@Configuration
public class NotificationConfig {

    @Bean
    public Map<NotificationChannel, NotificationDeliveryPort> notificationDeliveryPorts(
        @Qualifier("inAppNotificationDeliveryAdapter") NotificationDeliveryPort inAppAdapter,
        @Qualifier("emailNotificationDeliveryAdapter") NotificationDeliveryPort emailAdapter
    ) {
        Map<NotificationChannel, NotificationDeliveryPort> map = new EnumMap<>(NotificationChannel.class);
        map.put(NotificationChannel.IN_APP, inAppAdapter);
        map.put(NotificationChannel.EMAIL, emailAdapter);
        return map;
    }
}
