package com.transportlogistics.app.notification.infrastructure.adapters.in.web.controllers;

import com.transportlogistics.app.notification.application.ports.in.NotificationConfigurationUseCase;
import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.response.NotificationEventCatalogueResponse;
import com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.response.NotificationTemplateResponse;
import com.transportlogistics.app.notification.infrastructure.adapters.in.web.mappers.NotificationWebMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class NotificationConfigurationController {
    private final NotificationConfigurationUseCase configuration;
    private final NotificationWebMapper mapper;

    public NotificationConfigurationController(NotificationConfigurationUseCase configuration, NotificationWebMapper mapper) {
        this.configuration = configuration;
        this.mapper = mapper;
    }

    @GetMapping("/notification-event-catalogue")
    public List<NotificationEventCatalogueResponse> eventCatalogue() {
        return configuration.listEventCatalogue().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/notification-templates")
    public List<NotificationTemplateResponse> templates(
        @RequestParam(required = false) String eventType,
        @RequestParam(required = false) NotificationChannel channel
    ) {
        return configuration.listActiveTemplates(eventType, channel).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/notification-templates/{id}")
    public ResponseEntity<NotificationTemplateResponse> template(@PathVariable UUID id) {
        return configuration.getActiveTemplate(id)
            .map(mapper::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
