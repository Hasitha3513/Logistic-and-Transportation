package com.transportlogistics.app.fuel.infrastructure.adapters.out;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

import static org.assertj.core.api.Assertions.assertThat;

class E2eFuelOwnerCorrectionProfileSafetyTest {
    @Test void controlledFailureAdapterIsE2eOnly() {
        assertThat(E2eFailFirstFuelOwnerCorrection.class.getAnnotation(Profile.class).value())
                .containsExactly("e2e");
    }
}
