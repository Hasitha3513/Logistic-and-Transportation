package com.transportlogistics.app.notification.infrastructure.testing;

import com.transportlogistics.app.notification.infrastructure.adapters.in.web.controllers.E2eNotificationTestController;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.context.annotation.Profile;

import static org.assertj.core.api.Assertions.assertThat;

class E2eNotificationProfileSafetyTest {
    @Test
    void testControlsAreRestrictedToTheE2eProfile() {
        assertE2eOnly(E2eNotificationTestConfiguration.class);
        assertE2eOnly(E2eNotificationTestController.class);
    }

    private void assertE2eOnly(Class<?> type) {
        Profile profile = AnnotatedElementUtils.findMergedAnnotation(type, Profile.class);
        assertThat(profile).isNotNull();
        assertThat(profile.value()).containsExactly("e2e");
    }
}
