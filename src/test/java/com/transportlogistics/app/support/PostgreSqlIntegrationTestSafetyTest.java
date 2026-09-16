package com.transportlogistics.app.support;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PostgreSqlIntegrationTestSafetyTest {

    @Test
    void acceptsOnlyMatchingConnectedAcceptanceDatabaseEvidence() {
        assertThatCode(() -> AcceptanceDatabaseGuard.requireConnectedDatabase(
                "transport_logistics_acceptance", "transport_logistics_acceptance"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsDevelopmentMissingOtherAndUnverifiableDatabaseEvidence() {
        for (String database : new String[] {"transport_logistics", "transport_test", "other"}) {
            assertThatThrownBy(() -> AcceptanceDatabaseGuard.requireConnectedDatabase(database, database))
                    .isInstanceOf(IllegalStateException.class);
        }
        assertThatThrownBy(() -> AcceptanceDatabaseGuard.requireConnectedDatabase(null, null))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> AcceptanceDatabaseGuard.requireConnectedDatabase(
                "transport_logistics_acceptance", "transport_logistics"))
                .isInstanceOf(IllegalStateException.class);
    }
}
