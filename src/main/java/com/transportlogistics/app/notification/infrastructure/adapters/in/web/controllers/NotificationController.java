package com.transportlogistics.app.notification.infrastructure.adapters.in.web.controllers;

import com.transportlogistics.app.notification.application.ports.in.NotificationUseCase;
import com.transportlogistics.app.notification.domain.model.Notification;
import com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.response.NotificationResponse;
import com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.response.UnreadCountResponse;
import com.transportlogistics.app.notification.infrastructure.adapters.in.web.mappers.NotificationWebMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationUseCase notificationUseCase;
    private final NotificationWebMapper mapper;

    public NotificationController(NotificationUseCase notificationUseCase, NotificationWebMapper mapper) {
        this.notificationUseCase = notificationUseCase;
        this.mapper = mapper;
    }

    private Set<String> extractUserRoles(Authentication authentication) {
        if (authentication == null) {
            return Collections.emptySet();
        }
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> listNotifications(
        @RequestParam(defaultValue = "50") int limit,
        Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : "anonymous";
        Set<String> roles = extractUserRoles(authentication);
        List<NotificationResponse> responses = notificationUseCase.listNotificationsForUser(username, roles, limit).stream()
            .map(mapper::toResponse)
            .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "anonymous";
        Set<String> roles = extractUserRoles(authentication);
        long count = notificationUseCase.getUnreadCount(username, roles);
        return ResponseEntity.ok(new UnreadCountResponse(count));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
        @PathVariable UUID id,
        Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : "anonymous";
        Set<String> roles = extractUserRoles(authentication);
        Notification readNotification = notificationUseCase.markAsRead(id, username, roles);
        return ResponseEntity.ok(mapper.toResponse(readNotification));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "anonymous";
        Set<String> roles = extractUserRoles(authentication);
        notificationUseCase.markAllAsRead(username, roles);
        return ResponseEntity.noContent().build();
    }
}
