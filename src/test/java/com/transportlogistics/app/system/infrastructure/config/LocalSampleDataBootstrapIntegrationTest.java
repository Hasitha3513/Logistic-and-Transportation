package com.transportlogistics.app.system.infrastructure.config;

import com.transportlogistics.app.routing.application.ports.in.RouteUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = "app.dev.sample-data.enabled=true")
class LocalSampleDataBootstrapIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired RouteUseCase routes;

    @Test
    void loadsPhaseOneDatasetIncludingAnOrderedRouteStop() {
        assertEquals(3, count("vehicle"));
        assertEquals(3, count("driver"));
        assertEquals(2, count("route"));
        assertEquals(7, count("trip"));

        var route = routes.get(java.util.UUID.fromString("50000000-0000-0000-0000-000000000001"));
        assertEquals(1, route.stopLocationIds().size());
        assertEquals(java.util.UUID.fromString("20000000-0000-0000-0000-000000000004"),
                route.stopLocationIds().getFirst());
    }

    private int count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Integer.class);
    }
}
