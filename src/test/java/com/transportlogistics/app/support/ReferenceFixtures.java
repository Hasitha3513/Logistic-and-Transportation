package com.transportlogistics.app.support;

import com.transportlogistics.app.fleet.domain.model.Vehicle;
import com.transportlogistics.app.trip.domain.model.Trip;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.UUID;

public final class ReferenceFixtures {
    private ReferenceFixtures() {
    }

    public static void locations(JdbcTemplate jdbc, UUID... ids) {
        Arrays.stream(ids).distinct().forEach(id -> jdbc.update(
                "INSERT INTO location (id, code, name, active) VALUES (?, ?, ?, ?)",
                id, code("LOC", id), "Test location " + shortId(id), true));
    }

    public static void vehicleHierarchy(JdbcTemplate jdbc, Vehicle vehicle) {
        jdbc.update("INSERT INTO vehicle_category (id, code, name, active) VALUES (?, ?, ?, ?)",
                vehicle.categoryId(), code("CAT", vehicle.categoryId()), "Test category", true);
        jdbc.update("INSERT INTO vehicle_type (id, category_id, code, name, active) VALUES (?, ?, ?, ?, ?)",
                vehicle.typeId(), vehicle.categoryId(), code("TYPE", vehicle.typeId()), "Test type", true);
    }

    public static void vehicleReference(JdbcTemplate jdbc, UUID vehicleId) {
        var categoryId = UUID.randomUUID();
        var typeId = UUID.randomUUID();
        jdbc.update("INSERT INTO vehicle_category (id, code, name, active) VALUES (?, ?, ?, ?)",
                categoryId, code("CAT", categoryId), "Test category", true);
        jdbc.update("INSERT INTO vehicle_type (id, category_id, code, name, active) VALUES (?, ?, ?, ?, ?)",
                typeId, categoryId, code("TYPE", typeId), "Test type", true);
        jdbc.update("""
                INSERT INTO vehicle
                    (id, registration_number, category_id, type_id, ownership_type, operational_status, active)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, vehicleId, code("REG", vehicleId), categoryId, typeId, "OWNED", "AVAILABLE", true);
    }

    public static void driverReference(JdbcTemplate jdbc, UUID driverId) {
        jdbc.update("""
                INSERT INTO driver
                    (id, employee_number, first_name, last_name, status, active)
                VALUES (?, ?, ?, ?, ?, ?)
                """, driverId, code("EMP", driverId), "Test", "Driver", "AVAILABLE", true);
    }

    public static void tripLocations(JdbcTemplate jdbc, Trip trip) {
        locations(jdbc, trip.originLocationId(), trip.destinationLocationId());
    }

    public static UUID appUser(JdbcTemplate jdbc, String username) {
        var id = UUID.randomUUID();
        return appUser(jdbc, id, username);
    }

    public static UUID appUser(JdbcTemplate jdbc, UUID id, String username) {
        var now = java.time.OffsetDateTime.now();
        jdbc.update("""
                INSERT INTO app_user (id, username, email, password_hash, first_name, last_name, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, username, username + "@example.test", "hash", "Test", "User", true, now, now);
        return id;
    }

    private static String code(String prefix, UUID id) {
        return prefix + "-" + shortId(id);
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 12);
    }
}
