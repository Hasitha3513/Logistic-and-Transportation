package com.transportlogistics.app.notification.application.service;

import com.transportlogistics.app.notification.application.ports.in.CustomerNotificationPreferenceUseCase;
import com.transportlogistics.app.notification.application.ports.out.CustomerNotificationContactPort;
import com.transportlogistics.app.notification.application.ports.out.CustomerNotificationPreferenceRepository;
import com.transportlogistics.app.notification.domain.model.CustomerNotificationPreference;
import com.transportlogistics.app.notification.domain.model.NotificationDestination;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.tenancy.CurrentTenant;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class CustomerNotificationPreferenceService implements CustomerNotificationPreferenceUseCase {
    private final CustomerNotificationPreferenceRepository preferences;
    private final CustomerNotificationContactPort contacts;
    private final CurrentTenant currentTenant;
    private final Clock clock;

    public CustomerNotificationPreferenceService(CustomerNotificationPreferenceRepository preferences,
                                                 CustomerNotificationContactPort contacts,
                                                 CurrentTenant currentTenant,
                                                 Clock clock) {
        this.preferences = preferences;
        this.contacts = contacts;
        this.currentTenant = currentTenant;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PreferenceView get(UUID customerId) {
        var contact = activeContact(customerId);
        return view(contact, preferences.findByCustomerId(customerId).orElse(null));
    }

    @Override
    @Transactional
    public PreferenceView replace(UUID customerId, ReplaceCommand command) {
        if (command == null) throw new IllegalArgumentException("Preference body is required");
        var contact = activeContact(customerId);
        var existing = preferences.findByCustomerId(customerId);
        CustomerNotificationPreference preference;
        OffsetDateTime now = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        if (existing.isEmpty()) {
            if (command.version() != null) {
                throw conflict("Preference does not yet exist; version must be null");
            }
            preference = CustomerNotificationPreference.create(currentTenant.required().tenantId(), customerId,
                    command.emailEnabled(), command.smsEnabled(), now);
        } else {
            if (command.version() == null || command.version() != existing.get().version()) {
                throw conflict("Customer notification preference changed concurrently; reload and retry");
            }
            preference = existing.get().update(command.emailEnabled(), command.smsEnabled(), now);
        }
        try {
            return view(contact, preferences.save(preference));
        } catch (OptimisticLockingFailureException exception) {
            throw new ConflictException("NOTIFICATION_PREFERENCE_VERSION_CONFLICT",
                    "Customer notification preference changed concurrently; reload and retry", exception);
        }
    }


    private CustomerNotificationContactPort.CustomerContact activeContact(UUID customerId) {
        var contact = contacts.find(customerId).filter(CustomerNotificationContactPort.CustomerContact::active)
                .orElseThrow(() -> new NotFoundException("CUSTOMER_NOT_FOUND", "Customer was not found"));
        return contact;
    }

    private PreferenceView view(CustomerNotificationContactPort.CustomerContact contact,
                                CustomerNotificationPreference preference) {
        boolean explicit = preference != null;
        boolean emailEnabled = explicit ? preference.emailEnabled()
                : NotificationDestination.email(contact.email()).isPresent();
        boolean smsEnabled = explicit && preference.smsEnabled();
        return new PreferenceView(contact.customerId(), explicit, emailEnabled, smsEnabled,
                NotificationDestination.email(contact.email()).map(NotificationDestination::mask).orElse(null),
                NotificationDestination.sms(contact.phone()).map(NotificationDestination::mask).orElse(null),
                explicit ? preference.version() : null);
    }

    private static ConflictException conflict(String message) {
        return new ConflictException("NOTIFICATION_PREFERENCE_VERSION_CONFLICT", message);
    }
}
