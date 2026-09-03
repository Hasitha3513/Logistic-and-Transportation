package com.transportlogistics.app.notification.application.service;

import com.transportlogistics.app.notification.application.ports.out.NotificationRecipientDirectoryPort;
import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.RecipientType;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class NotificationRecipientResolver {
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private final NotificationRecipientDirectoryPort directory;

    public NotificationRecipientResolver(NotificationRecipientDirectoryPort directory) {
        this.directory = Objects.requireNonNull(directory, "directory must not be null");
    }

    public void validate(RecipientType type, NotificationChannel channel, String value) {
        Objects.requireNonNull(type, "recipient type must not be null");
        Objects.requireNonNull(channel, "channel must not be null");
        String recipient = required(value);
        switch (type) {
            case EMAIL_ADDRESS -> {
                if (channel != NotificationChannel.EMAIL) {
                    incompatible("EMAIL_ADDRESS recipients support EMAIL only");
                }
                validateEmail(recipient);
            }
            case USER -> {
                var user = directory.findActiveUser(recipient)
                    .orElseThrow(() -> new BusinessRuleException("NOTIFICATION_RECIPIENT_NOT_FOUND",
                        "Active notification user not found: " + recipient));
                if (channel == NotificationChannel.EMAIL) {
                    validateUserEmail(user);
                }
            }
            case ROLE -> {
                if (!directory.activeRoleExists(recipient)) {
                    throw new BusinessRuleException("NOTIFICATION_RECIPIENT_NOT_FOUND",
                        "Active notification role not found: " + recipient);
                }
            }
            case EVENT_CUSTOMER -> {
                if (channel != NotificationChannel.EMAIL && channel != NotificationChannel.SMS) {
                    incompatible("EVENT_CUSTOMER recipients support EMAIL and SMS only");
                }
                if (!"customerId".equals(recipient)) {
                    throw new BusinessRuleException("NOTIFICATION_RECIPIENT_INVALID",
                        "EVENT_CUSTOMER recipient value must be customerId");
                }
            }
        }
    }

    public List<String> resolve(RecipientType type, NotificationChannel channel, String value) {
        validate(type, channel, value);
        String recipient = value.trim();
        return switch (type) {
            case EMAIL_ADDRESS -> List.of(normalizeEmail(recipient));
            case USER -> {
                var user = directory.findActiveUser(recipient).orElseThrow();
                yield channel == NotificationChannel.IN_APP
                    ? List.of(user.username())
                    : List.of(normalizeEmail(validateUserEmail(user)));
            }
            case ROLE -> directory.findActiveRoleMembers(recipient).stream()
                .map(user -> channel == NotificationChannel.IN_APP ? user.username() : usableEmail(user))
                .filter(Objects::nonNull)
                .map(valueToNormalize -> channel == NotificationChannel.EMAIL ? normalizeEmail(valueToNormalize) : valueToNormalize)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
            case EVENT_CUSTOMER -> throw new BusinessRuleException("NOTIFICATION_RECIPIENT_EVENT_REQUIRED",
                "EVENT_CUSTOMER recipients must be resolved from an event");
        };
    }

    private static String usableEmail(NotificationRecipientDirectoryPort.RecipientUser user) {
        if (user.email() == null || !EMAIL.matcher(user.email().trim()).matches()) {
            return null;
        }
        return user.email();
    }

    private static String validateUserEmail(NotificationRecipientDirectoryPort.RecipientUser user) {
        if (user.email() == null || !EMAIL.matcher(user.email().trim()).matches()) {
            throw new BusinessRuleException("NOTIFICATION_RECIPIENT_INVALID",
                "Active user has no valid email address: " + user.username());
        }
        return user.email();
    }

    private static void validateEmail(String email) {
        if (!EMAIL.matcher(email).matches()) {
            throw new BusinessRuleException("NOTIFICATION_RECIPIENT_INVALID", "Invalid notification email address");
        }
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String required(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessRuleException("NOTIFICATION_RECIPIENT_INVALID", "Notification recipient is required");
        }
        return value.trim();
    }

    private static void incompatible(String message) {
        throw new BusinessRuleException("NOTIFICATION_CHANNEL_RECIPIENT_INCOMPATIBLE", message);
    }
}
