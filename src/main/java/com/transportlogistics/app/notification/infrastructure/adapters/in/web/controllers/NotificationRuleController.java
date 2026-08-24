package com.transportlogistics.app.notification.infrastructure.adapters.in.web.controllers;

import com.transportlogistics.app.notification.application.ports.in.NotificationRuleUseCase;
import com.transportlogistics.app.notification.domain.model.NotificationRule;
import com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.request.CreateNotificationRuleRequest;
import com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.request.UpdateNotificationRuleRequest;
import com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.response.NotificationRuleResponse;
import com.transportlogistics.app.notification.infrastructure.adapters.in.web.mappers.NotificationWebMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notification-rules")
public class NotificationRuleController {
    private final NotificationRuleUseCase ruleUseCase;
    private final NotificationWebMapper mapper;

    public NotificationRuleController(NotificationRuleUseCase ruleUseCase, NotificationWebMapper mapper) {
        this.ruleUseCase = ruleUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<List<NotificationRuleResponse>> listRules() {
        List<NotificationRuleResponse> responses = ruleUseCase.listRules().stream()
            .map(mapper::toResponse)
            .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationRuleResponse> getRule(@PathVariable UUID id) {
        return ruleUseCase.getRule(id)
            .map(mapper::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<NotificationRuleResponse> createRule(@Valid @RequestBody CreateNotificationRuleRequest request) {
        NotificationRule created = ruleUseCase.createRule(mapper.toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NotificationRuleResponse> updateRule(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateNotificationRuleRequest request
    ) {
        NotificationRule updated = ruleUseCase.updateRule(id, mapper.toCommand(request));
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @PatchMapping("/{id}/enable")
    public ResponseEntity<NotificationRuleResponse> enableRule(@PathVariable UUID id) {
        NotificationRule enabled = ruleUseCase.enableRule(id);
        return ResponseEntity.ok(mapper.toResponse(enabled));
    }

    @PatchMapping("/{id}/disable")
    public ResponseEntity<NotificationRuleResponse> disableRule(@PathVariable UUID id) {
        NotificationRule disabled = ruleUseCase.disableRule(id);
        return ResponseEntity.ok(mapper.toResponse(disabled));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID id) {
        ruleUseCase.deleteRule(id);
        return ResponseEntity.noContent().build();
    }
}
