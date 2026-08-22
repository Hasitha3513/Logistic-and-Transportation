package com.transportlogistics.app.notification.application.service;

import com.transportlogistics.app.notification.application.ports.out.NotificationDeliveryAttemptRepository;
import com.transportlogistics.app.notification.application.ports.out.NotificationRepository;
import com.transportlogistics.app.notification.domain.model.NotificationStatus;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class NotificationDeliveryDiagnosticsServiceTest {
    @Test void boundsOperationalQueryAndReturns404ForUnknownDelivery() {
        NotificationRepository notifications = mock(NotificationRepository.class);
        NotificationDeliveryAttemptRepository attempts = mock(NotificationDeliveryAttemptRepository.class);
        var service = new NotificationDeliveryDiagnosticsService(notifications, attempts);
        when(notifications.findDeliveries(NotificationStatus.FAILED, "TRIP_DELAY_RECORDED", null, null, 200))
            .thenReturn(List.of());
        service.find(NotificationStatus.FAILED, "TRIP_DELAY_RECORDED", null, null, 5000);
        verify(notifications).findDeliveries(NotificationStatus.FAILED, "TRIP_DELAY_RECORDED", null, null, 200);

        UUID missing = UUID.randomUUID();
        when(notifications.findById(missing)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.attempts(missing))
            .isInstanceOfSatisfying(NotFoundException.class,
                exception -> org.assertj.core.api.Assertions.assertThat(exception.code())
                    .isEqualTo("NOTIFICATION_DELIVERY_NOT_FOUND"));
    }
}
