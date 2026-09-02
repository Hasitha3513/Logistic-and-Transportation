package com.transportlogistics.app.notification.application.service;

import com.transportlogistics.app.notification.OperationalNotificationEvent;
import com.transportlogistics.app.notification.application.ports.out.CustomerNotificationContactPort;
import com.transportlogistics.app.notification.application.ports.out.CustomerNotificationPreferenceRepository;
import com.transportlogistics.app.notification.domain.model.CustomerNotificationPreference;
import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.tenancy.CurrentTenant;
import com.transportlogistics.app.tenancy.TenantExecutionContext;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventCustomerRecipientResolverTest {
    private final UUID tenantId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();
    private final CurrentTenant currentTenant = () -> Optional.of(new TenantExecutionContext(
        tenantId, UUID.randomUUID(), "tester", "correlation"));

    @Test
    void appliesDefaultEmailOnAndSmsOff() {
        var resolver = resolver(contact(true, "+947700000001", "CUSTOMER@EXAMPLE.TEST"), Optional.empty());

        assertThat(resolver.resolve(event(tenantId), NotificationChannel.EMAIL))
            .satisfies(result -> {
                assertThat(result.state()).isEqualTo(EventCustomerRecipientResolver.State.ACCEPTED);
                assertThat(result.recipient()).isEqualTo("customer@example.test");
            });
        assertThat(resolver.resolve(event(tenantId), NotificationChannel.SMS).state())
            .isEqualTo(EventCustomerRecipientResolver.State.SUPPRESSED);
    }

    @Test
    void explicitProfileIsAuthoritativeForEmailAndSms() {
        var resolver = resolver(contact(true, "+94 77-000-0001", "customer@example.test"),
            Optional.of(preference(false, true)));

        assertThat(resolver.resolve(event(tenantId), NotificationChannel.EMAIL).state())
            .isEqualTo(EventCustomerRecipientResolver.State.SUPPRESSED);
        assertThat(resolver.resolve(event(tenantId), NotificationChannel.SMS).recipient())
            .isEqualTo("+94770000001");
    }

    @Test
    void missingDestinationAndInactiveCustomerProduceNoRecipient() {
        assertThat(resolver(contact(true, null, null), Optional.empty())
            .resolve(event(tenantId), NotificationChannel.EMAIL).state())
            .isEqualTo(EventCustomerRecipientResolver.State.NO_RECIPIENT);
        assertThat(resolver(contact(false, "+947700000001", "customer@example.test"), Optional.empty())
            .resolve(event(tenantId), NotificationChannel.EMAIL).state())
            .isEqualTo(EventCustomerRecipientResolver.State.NO_RECIPIENT);
    }

    @Test
    void crossTenantEventNeverResolvesCustomerDestination() {
        var resolver = resolver(contact(true, "+947700000001", "customer@example.test"), Optional.empty());

        assertThat(resolver.resolve(event(UUID.randomUUID()), NotificationChannel.EMAIL).state())
            .isEqualTo(EventCustomerRecipientResolver.State.NO_RECIPIENT);
    }

    private EventCustomerRecipientResolver resolver(Optional<CustomerNotificationContactPort.CustomerContact> contact,
                                                     Optional<CustomerNotificationPreference> preference) {
        CustomerNotificationContactPort contacts = ignored -> contact;
        CustomerNotificationPreferenceRepository preferences = new CustomerNotificationPreferenceRepository() {
            @Override
            public Optional<CustomerNotificationPreference> findByCustomerId(UUID ignored) {
                return preference;
            }

            @Override
            public CustomerNotificationPreference save(CustomerNotificationPreference value) {
                return value;
            }
        };
        return new EventCustomerRecipientResolver(contacts, preferences, currentTenant);
    }

    private Optional<CustomerNotificationContactPort.CustomerContact> contact(boolean active, String phone,
                                                                               String email) {
        return Optional.of(new CustomerNotificationContactPort.CustomerContact(
            customerId, active, "Customer", phone, email));
    }

    private CustomerNotificationPreference preference(boolean email, boolean sms) {
        return CustomerNotificationPreference.create(tenantId, customerId, email, sms, OffsetDateTime.now());
    }

    private OperationalNotificationEvent event(UUID eventTenantId) {
        return new OperationalNotificationEvent(UUID.randomUUID(), "DELIVERY_COMPLETED", "DELIVERY_ORDER",
            UUID.randomUUID(), OperationalNotificationEvent.Severity.INFO, "Delivery completed",
            "Delivery completed", OffsetDateTime.now(), Map.of("customerId", customerId.toString()),
            eventTenantId, 1);
    }
}
