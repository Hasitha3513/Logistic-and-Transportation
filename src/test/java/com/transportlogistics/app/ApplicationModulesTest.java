package com.transportlogistics.app;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationModulesTest {
    @Test
    void verifiesModuleBoundaries() {
        ApplicationModules.of(TransportLogisticsApplication.class).verify();
    }

    @Test
    void discoversDeliveryModule() {
        var modules = ApplicationModules.of(TransportLogisticsApplication.class);

        assertThat(modules.stream().map(module -> module.getIdentifier().toString()))
                .contains("delivery");
    }

    @Test
    void printModules() {
        ApplicationModules.of(TransportLogisticsApplication.class).forEach(System.out::println);
    }
}
