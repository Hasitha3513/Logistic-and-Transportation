package com.transportlogistics.app.operations.adapters.inbound.web.controllers;

import com.transportlogistics.app.delivery.adapters.inbound.web.controllers.E2eDeliveryExceptionFixtureController;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

import static org.assertj.core.api.Assertions.assertThat;

class E2eOperationalExceptionProfileSafetyTest {
    @Test
    void controlsAreAvailableOnlyInTheE2eProfile() {
        assertThat(E2eOperationalExceptionTestController.class.getAnnotation(Profile.class).value())
            .containsExactly("e2e");
        assertThat(E2eDeliveryExceptionFixtureController.class.getAnnotation(Profile.class).value())
            .containsExactly("e2e");
    }
}
