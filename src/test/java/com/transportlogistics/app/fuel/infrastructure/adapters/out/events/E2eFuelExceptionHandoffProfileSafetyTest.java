package com.transportlogistics.app.fuel.infrastructure.adapters.out.events;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

import static org.assertj.core.api.Assertions.assertThat;

class E2eFuelExceptionHandoffProfileSafetyTest {
    @Test void controlledFailureAdapterIsE2eOnly() {
        assertThat(E2eFailFirstFuelExceptionHandoff.class.getAnnotation(Profile.class).value())
                .containsExactly("e2e");
    }
}
