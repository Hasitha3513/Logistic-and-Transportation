package com.transportlogistics.app.offlinesync.infrastructure.testing;

import com.transportlogistics.app.offlinesync.infrastructure.adapters.in.web.controllers.E2eOfflineSyncTestController;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.AnnotatedElementUtils;

import static org.assertj.core.api.Assertions.assertThat;

class E2eOfflineSyncProfileSafetyTest {
    @Test
    void controlsAreRestrictedToTheE2eProfile() {
        assertE2eOnly(E2eOfflineSyncTestConfiguration.class);
        assertE2eOnly(E2eOfflineSyncTestController.class);
    }

    private void assertE2eOnly(Class<?> type) {
        Profile profile = AnnotatedElementUtils.findMergedAnnotation(type, Profile.class);
        assertThat(profile).isNotNull();
        assertThat(profile.value()).containsExactly("e2e");
    }
}
