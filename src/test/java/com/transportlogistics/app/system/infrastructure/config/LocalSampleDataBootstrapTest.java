package com.transportlogistics.app.system.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalSampleDataBootstrapTest {

    @Test
    void selectsPostgresqlFixtureForPostgresStartup() {
        var environment = new MockEnvironment().withProperty("spring.profiles.active", "postgres");
        environment.setActiveProfiles("postgres");
        var bootstrap = new LocalSampleDataBootstrap(null, environment);

        assertEquals("db/sample-data/postgresql-sample-data.sql", bootstrap.sampleDataScript().getPath());
    }

    @Test
    void retainsH2FixtureForH2Startup() {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("h2");
        var bootstrap = new LocalSampleDataBootstrap(null, environment);

        assertEquals("db/sample-data/h2-phase1.sql", bootstrap.sampleDataScript().getPath());
    }
}
