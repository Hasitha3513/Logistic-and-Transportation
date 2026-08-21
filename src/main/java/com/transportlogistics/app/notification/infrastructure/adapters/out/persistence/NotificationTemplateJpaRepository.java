package com.transportlogistics.app.notification.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface NotificationTemplateJpaRepository extends JpaRepository<NotificationTemplateEntity, UUID> {
    List<NotificationTemplateEntity> findByCodeIgnoreCaseAndEventTypeIgnoreCaseAndChannelAndActiveTrue(
        String code, String eventType, NotificationChannel channel);

    List<NotificationTemplateEntity> findByActiveTrueOrderByEventTypeAscChannelAscVersionDesc();

    List<NotificationTemplateEntity> findByEventTypeIgnoreCaseAndActiveTrueOrderByChannelAscVersionDesc(String eventType);

    List<NotificationTemplateEntity> findByChannelAndActiveTrueOrderByEventTypeAscVersionDesc(NotificationChannel channel);

    List<NotificationTemplateEntity> findByEventTypeIgnoreCaseAndChannelAndActiveTrueOrderByVersionDesc(
        String eventType, NotificationChannel channel);
}
