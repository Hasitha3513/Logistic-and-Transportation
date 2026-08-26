package com.transportlogistics.app.fleet.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LubricantLogTest {

    private final UUID id = UUID.randomUUID();
    private final UUID vehicleId = UUID.randomUUID();
    private final OffsetDateTime now = OffsetDateTime.now();

    @Test
    @DisplayName("Should create valid lubricant log record")
    void shouldCreateValidLubricantLog() {
        var log = new LubricantLog(
                id,
                vehicleId,
                FluidType.ENGINE_OIL,
                new BigDecimal("15.50"),
                MeasurementUnit.LITRE,
                now,
                55000.0,
                1200.0,
                UUID.randomUUID(),
                "Mobil Official Supplier",
                "REF-LUB-100",
                "15W-40 Synthetic Blend",
                true,
                now,
                now,
                "mechanic",
                "mechanic"
        );

        assertEquals(id, log.id());
        assertEquals(vehicleId, log.vehicleId());
        assertEquals(FluidType.ENGINE_OIL, log.fluidType());
        assertEquals(new BigDecimal("15.50"), log.quantity());
        assertEquals(MeasurementUnit.LITRE, log.unit());
        assertEquals(55000.0, log.odometerKm());
        assertEquals(1200.0, log.engineHours());
        assertTrue(log.active());
    }

    @ParameterizedTest
    @EnumSource(FluidType.class)
    @DisplayName("Should support all fluid types")
    void shouldSupportAllFluidTypes(FluidType fluidType) {
        var log = new LubricantLog(
                id,
                vehicleId,
                fluidType,
                new BigDecimal("5.00"),
                MeasurementUnit.LITRE,
                now,
                null,
                null,
                null,
                null,
                null,
                fluidType == FluidType.OTHER ? "Special additive" : null,
                true,
                now,
                now,
                "mechanic",
                "mechanic"
        );

        assertEquals(fluidType, log.fluidType());
    }

    @Test
    @DisplayName("Should throw when quantity is zero or negative")
    void shouldThrowWhenQuantityZeroOrNegative() {
        assertThrows(IllegalArgumentException.class, () -> new LubricantLog(
                id, vehicleId, FluidType.ENGINE_OIL, BigDecimal.ZERO, MeasurementUnit.LITRE,
                now, null, null, null, null, null, null, true, now, now, "admin", "admin"
        ));

        assertThrows(IllegalArgumentException.class, () -> new LubricantLog(
                id, vehicleId, FluidType.ENGINE_OIL, new BigDecimal("-5.0"), MeasurementUnit.LITRE,
                now, null, null, null, null, null, null, true, now, now, "admin", "admin"
        ));
    }

    @Test
    @DisplayName("Should throw when odometer or engine hours are negative")
    void shouldThrowWhenOdometerOrEngineHoursNegative() {
        assertThrows(IllegalArgumentException.class, () -> new LubricantLog(
                id, vehicleId, FluidType.ENGINE_OIL, new BigDecimal("5.0"), MeasurementUnit.LITRE,
                now, -10.0, null, null, null, null, null, true, now, now, "admin", "admin"
        ));

        assertThrows(IllegalArgumentException.class, () -> new LubricantLog(
                id, vehicleId, FluidType.ENGINE_OIL, new BigDecimal("5.0"), MeasurementUnit.LITRE,
                now, null, -5.0, null, null, null, null, true, now, now, "admin", "admin"
        ));
    }

    @Test
    @DisplayName("Should throw when recorded date is in the far future")
    void shouldThrowWhenRecordedDateInFuture() {
        var future = OffsetDateTime.now().plusDays(2);
        assertThrows(IllegalArgumentException.class, () -> new LubricantLog(
                id, vehicleId, FluidType.ENGINE_OIL, new BigDecimal("5.0"), MeasurementUnit.LITRE,
                future, null, null, null, null, null, null, true, now, now, "admin", "admin"
        ));
    }
}
