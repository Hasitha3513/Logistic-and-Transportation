package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.ports.outbound.DeliveryNumberGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Year;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DeliveryNumberGeneratorIntegrationTest {

    @Autowired
    private DeliveryNumberGenerator generator;

    @Test
    void generatesSequentialDeliveryNumbersScopedByTenantAndYear() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        var numA1 = generator.next(tenantA, Year.of(2026));
        var numA2 = generator.next(tenantA, Year.of(2026));
        var numB1 = generator.next(tenantB, Year.of(2026));
        var numA_2027 = generator.next(tenantA, Year.of(2027));

        assertThat(numA1.value()).isEqualTo("DEL-2026-000001");
        assertThat(numA2.value()).isEqualTo("DEL-2026-000002");
        assertThat(numB1.value()).isEqualTo("DEL-2026-000001");
        assertThat(numA_2027.value()).isEqualTo("DEL-2027-000001");
    }
}
