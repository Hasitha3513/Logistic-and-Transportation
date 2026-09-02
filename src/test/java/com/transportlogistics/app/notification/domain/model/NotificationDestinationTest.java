package com.transportlogistics.app.notification.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationDestinationTest {
    @Test
    void normalizesValidDestinationsAndRejectsInvalidOnes() {
        assertThat(NotificationDestination.email(" Ops@Example.Test ")).contains("ops@example.test");
        assertThat(NotificationDestination.email("not-an-email")).isEmpty();
        assertThat(NotificationDestination.sms("+94 (77) 123-4567")).contains("+94771234567");
        assertThat(NotificationDestination.sms("0771234567")).isEmpty();
    }

    @Test
    void masksEmailAndPhoneDestinations() {
        assertThat(NotificationDestination.mask("ops@example.test")).isEqualTo("o***@example.test");
        assertThat(NotificationDestination.mask("+94771234567")).isEqualTo("+94***67");
    }
}
