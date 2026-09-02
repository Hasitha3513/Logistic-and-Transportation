package com.transportlogistics.app.notification.application.service;

import com.transportlogistics.app.notification.application.ports.in.CustomerNotificationPreferenceUseCase;
import com.transportlogistics.app.notification.application.ports.out.CustomerNotificationContactPort;
import com.transportlogistics.app.notification.application.ports.out.CustomerNotificationPreferenceRepository;
import com.transportlogistics.app.notification.domain.model.CustomerNotificationPreference;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.tenancy.CurrentTenant;
import com.transportlogistics.app.tenancy.TenantExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerNotificationPreferenceServiceTest {
    private final UUID tenantId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();
    private final Map<UUID, CustomerNotificationPreference> stored = new HashMap<>();
    private CustomerNotificationContactPort contacts;
    private CustomerNotificationPreferenceService service;

    @BeforeEach
    void setUp() {
        contacts = id -> id.equals(customerId)
            ? Optional.of(new CustomerNotificationContactPort.CustomerContact(id, true, "Customer",
                "+947700000001", "CUSTOMER@EXAMPLE.TEST")) : Optional.empty();
        CustomerNotificationPreferenceRepository preferences = new CustomerNotificationPreferenceRepository() {
            @Override public Optional<CustomerNotificationPreference> findByCustomerId(UUID id) {
                return Optional.ofNullable(stored.get(id));
            }
            @Override public CustomerNotificationPreference save(CustomerNotificationPreference preference) {
                stored.put(preference.customerId(), preference);
                return preference;
            }
        };
        CurrentTenant currentTenant = () -> Optional.of(new TenantExecutionContext(tenantId, UUID.randomUUID(),
            "tester", "correlation"));
        service = new CustomerNotificationPreferenceService(preferences, contacts, currentTenant,
            Clock.fixed(Instant.parse("2026-09-02T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void defaultsEmailOnForValidContactAndSmsOffWithoutProfile() {
        var result = service.get(customerId);
        assertThat(result.explicitProfile()).isFalse();
        assertThat(result.emailEnabled()).isTrue();
        assertThat(result.smsEnabled()).isFalse();
        assertThat(result.maskedEmail()).isEqualTo("c***@example.test");
        assertThat(result.maskedPhone()).isEqualTo("+94***01");
    }

    @Test
    void explicitProfileIsAuthoritativeAndStaleVersionConflicts() {
        var created = service.replace(customerId,
            new CustomerNotificationPreferenceUseCase.ReplaceCommand(false, true, null));
        assertThat(created.explicitProfile()).isTrue();
        assertThat(created.emailEnabled()).isFalse();
        assertThat(created.smsEnabled()).isTrue();
        assertThatThrownBy(() -> service.replace(customerId,
            new CustomerNotificationPreferenceUseCase.ReplaceCommand(true, false, 1L)))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    void unknownOrCrossTenantCustomerIsSafeNotFound() {
        assertThatThrownBy(() -> service.get(UUID.randomUUID())).isInstanceOf(NotFoundException.class);
    }
}
