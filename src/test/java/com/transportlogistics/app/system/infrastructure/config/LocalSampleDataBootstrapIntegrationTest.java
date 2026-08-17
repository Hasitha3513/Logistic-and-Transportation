package com.transportlogistics.app.system.infrastructure.config;

import com.transportlogistics.app.routing.application.ports.in.RouteUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = "app.dev.sample-data.enabled=true")
@DirtiesContext
class LocalSampleDataBootstrapIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired RouteUseCase routes;
    @Autowired javax.sql.DataSource dataSource;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM trip_status_history");
        jdbc.update("DELETE FROM trip_dispatch");
        jdbc.update("DELETE FROM trip");
        jdbc.update("DELETE FROM route_stop");
        jdbc.update("DELETE FROM route");
        jdbc.update("DELETE FROM driver_license");
        jdbc.update("DELETE FROM driver");
        jdbc.update("DELETE FROM vehicle_document");
        jdbc.update("DELETE FROM vehicle_meter_reset");
        jdbc.update("DELETE FROM vehicle_reading");
        jdbc.update("DELETE FROM vehicle");
        jdbc.update("DELETE FROM vehicle_type");
        jdbc.update("DELETE FROM vehicle_category");
        jdbc.update("DELETE FROM fuel_station");
        jdbc.update("DELETE FROM project");
        jdbc.update("DELETE FROM department");
        jdbc.update("DELETE FROM location");
        jdbc.update("DELETE FROM customer");

        var script = new org.springframework.core.io.ClassPathResource("db/sample-data/h2-phase1.sql");
        var populator = new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(script);
        populator.setSeparator(";");
        populator.setContinueOnError(false);
        org.springframework.jdbc.datasource.init.DatabasePopulatorUtils.execute(populator, dataSource);
    }

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
