package com.transportlogistics.app.integration.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrationExchangeTest {
    @Test
    void frozenRetryScheduleIsBoundedToFiveAttempts() {
        assertThat(IntegrationExchange.MAX_ATTEMPTS).isEqualTo(5);
        assertThat(IntegrationExchange.backoffAfterAttempt(1)).hasSeconds(30);
        assertThat(IntegrationExchange.backoffAfterAttempt(2)).hasMinutes(2);
        assertThat(IntegrationExchange.backoffAfterAttempt(3)).hasMinutes(10);
        assertThat(IntegrationExchange.backoffAfterAttempt(4)).hasMinutes(30);
        assertThat(IntegrationExchange.backoffAfterAttempt(5)).isZero();
    }
}
