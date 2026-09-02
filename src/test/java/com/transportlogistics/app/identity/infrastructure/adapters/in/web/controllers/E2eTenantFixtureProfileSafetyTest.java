package com.transportlogistics.app.identity.infrastructure.adapters.in.web.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

import static org.assertj.core.api.Assertions.assertThat;

class E2eTenantFixtureProfileSafetyTest {
    @Test void fixture_endpoint_is_restricted_to_e2e_profile() {
        assertThat(E2eTenantFixtureController.class.getAnnotation(Profile.class).value()).containsExactly("e2e");
    }
}
