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
        cleanTables();

        var script = new org.springframework.core.io.ClassPathResource("db/sample-data/h2-phase1.sql");
        var populator = new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(script);
        populator.setSeparator(";");
        populator.setContinueOnError(false);
        org.springframework.jdbc.datasource.init.DatabasePopulatorUtils.execute(populator, dataSource);
    }

    private void cleanTables() {
        jdbc.update("DELETE FROM freight_insurance_settlement");
        jdbc.update("DELETE FROM freight_insurance_claim");
        jdbc.update("DELETE FROM freight_insurance_policy");
        jdbc.update("DELETE FROM load_plan_item_placement");
        jdbc.update("DELETE FROM load_plan");
        jdbc.update("DELETE FROM cargo_manifest_item");
        jdbc.update("DELETE FROM cargo_manifest");
        jdbc.update("DELETE FROM freight_order_line");
        jdbc.update("DELETE FROM freight_order");
        jdbc.update("DELETE FROM offline_sync_operation");
        jdbc.update("DELETE FROM trip_operational_event");
        jdbc.update("DELETE FROM trip_status_history");
        jdbc.update("DELETE FROM trip_dispatch");
        jdbc.update("DELETE FROM fuel_issue_history");
        jdbc.update("DELETE FROM fuel_issue");
        jdbc.update("DELETE FROM fuel_purchase_history");
        jdbc.update("DELETE FROM fuel_purchase");
        jdbc.update("DELETE FROM trip");
        jdbc.update("DELETE FROM route_disruption");
        jdbc.update("DELETE FROM route_stop");
        jdbc.update("DELETE FROM route");
        jdbc.update("DELETE FROM driver_license");
        jdbc.update("DELETE FROM driver_exception");
        jdbc.update("DELETE FROM driver_violation");
        jdbc.update("DELETE FROM driver_medical_record");
        jdbc.update("DELETE FROM driver_drug_test");
        jdbc.update("DELETE FROM driver");
        jdbc.update("DELETE FROM vehicle_document");
        jdbc.update("DELETE FROM vehicle_meter_reset");
        jdbc.update("DELETE FROM vehicle_reading");
        jdbc.update("DELETE FROM lubricant_log");
        jdbc.update("DELETE FROM fuel_limit_policy");
        jdbc.update("DELETE FROM bunker_stock_adjustment");
        jdbc.update("DELETE FROM bunker_dip_reading");
        jdbc.update("DELETE FROM bunker_stock_movement");
        jdbc.update("DELETE FROM bunker_tank");
        jdbc.update("DELETE FROM fuel_station");
        jdbc.update("DELETE FROM fuel_price");
        jdbc.update("DELETE FROM vendor");
        jdbc.update("DELETE FROM maintenance_schedule");
        jdbc.update("DELETE FROM vehicle");
        jdbc.update("DELETE FROM vehicle_type");
        jdbc.update("DELETE FROM vehicle_category");
        jdbc.update("DELETE FROM project");
        jdbc.update("DELETE FROM department");
        jdbc.update("DELETE FROM location");
        jdbc.update("DELETE FROM customer");
        jdbc.update("DELETE FROM app_user WHERE id IN ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000003')");
    }

    @Test
    void loadsPhaseOneDatasetIncludingAnOrderedRouteStop() {
        assertEquals(5, count("vehicle"));
        assertEquals(5, count("driver"));
        assertEquals(4, count("route"));
        assertEquals(8, count("trip"));
        assertEquals(3, count("vendor"));
        assertEquals(4, count("fuel_price"));
        assertEquals(4, count("fuel_station"));
        assertEquals(4, count("fuel_limit_policy"));
        assertEquals(3, count("bunker_tank"));
        assertEquals(6, count("bunker_stock_movement"));

        var route = routes.get(java.util.UUID.fromString("50000000-0000-0000-0000-000000000001"));
        assertEquals(1, route.stopLocationIds().size());
        assertEquals(java.util.UUID.fromString("20000000-0000-0000-0000-000000000004"),
                route.stopLocationIds().getFirst());
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        cleanTables();
    }

    private int count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Integer.class);
    }
}
