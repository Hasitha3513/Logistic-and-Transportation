package com.transportlogistics.app.notification.application.service;

import com.transportlogistics.app.notification.OperationalNotificationEvent;
import com.transportlogistics.app.notification.application.ports.out.CustomerNotificationContactPort;
import com.transportlogistics.app.notification.application.ports.out.CustomerNotificationPreferenceRepository;
import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationDestination;
import com.transportlogistics.app.tenancy.CurrentTenant;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class EventCustomerRecipientResolver {
    private final CustomerNotificationContactPort contacts;
    private final CustomerNotificationPreferenceRepository preferences;
    private final CurrentTenant currentTenant;

    public EventCustomerRecipientResolver(CustomerNotificationContactPort contacts,
                                          CustomerNotificationPreferenceRepository preferences,
                                          CurrentTenant currentTenant) {
        this.contacts = contacts;
        this.preferences = preferences;
        this.currentTenant = currentTenant;
    }

    public Resolution resolve(OperationalNotificationEvent event, NotificationChannel channel) {
        if (!currentTenant.required().tenantId().equals(event.tenantId())) return Resolution.noRecipient();
        UUID customerId;
        try {
            customerId = UUID.fromString(event.metadata().get("customerId"));
        } catch (RuntimeException exception) {
            return Resolution.noRecipient();
        }
        var contact = contacts.find(customerId).filter(CustomerNotificationContactPort.CustomerContact::active)
                .orElse(null);
        if (contact == null) return Resolution.noRecipient();
        var destination = switch (channel) {
            case EMAIL -> NotificationDestination.email(contact.email());
            case SMS -> NotificationDestination.sms(contact.phone());
            case IN_APP -> java.util.Optional.<String>empty();
        };
        if (destination.isEmpty()) return Resolution.noRecipient();
        var preference = preferences.findByCustomerId(customerId).orElse(null);
        boolean enabled = switch (channel) {
            case EMAIL -> preference == null
                    ? NotificationDestination.email(contact.email()).isPresent()
                    : preference.emailEnabled();
            case SMS -> preference != null && preference.smsEnabled();
            case IN_APP -> false;
        };
        if (!enabled) return Resolution.suppressed();
        return destination.map(value -> Resolution.accepted(value,
                        Map.of("customerDisplayName", safeDisplayName(contact.displayName()))))
                .orElseGet(Resolution::noRecipient);
    }

    private static String safeDisplayName(String value) {
        if (value == null || value.isBlank()) return "Customer";
        String trimmed = value.trim();
        return trimmed.length() <= 160 ? trimmed : trimmed.substring(0, 160);
    }

    public record Resolution(State state, String recipient, Map<String, String> variables) {
        public static Resolution accepted(String recipient, Map<String, String> variables) {
            return new Resolution(State.ACCEPTED, recipient, Map.copyOf(variables));
        }
        public static Resolution suppressed() { return new Resolution(State.SUPPRESSED, null, Map.of()); }
        public static Resolution noRecipient() { return new Resolution(State.NO_RECIPIENT, null, Map.of()); }
    }

    public enum State { ACCEPTED, SUPPRESSED, NO_RECIPIENT }
}
