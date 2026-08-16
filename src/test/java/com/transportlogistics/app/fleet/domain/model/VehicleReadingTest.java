package com.transportlogistics.app.fleet.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VehicleReadingTest {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-16T08:00:00Z");

    @Test
    void enforcesCanonicalUnitAndExactScale() {
        var reading = reading(new BigDecimal("123.450"), VehicleReadingUnit.KILOMETER);

        assertEquals(new BigDecimal("123.450"), reading.value());
        var error = assertThrows(BusinessRuleException.class,
                () -> reading(BigDecimal.ONE, VehicleReadingUnit.HOUR));
        assertEquals("INVALID_VEHICLE_READING", error.code());
    }

    @Test
    void rejectsNegativeAndExcessPrecisionValues() {
        assertThrows(BusinessRuleException.class,
                () -> reading(new BigDecimal("-0.001"), VehicleReadingUnit.KILOMETER));
        assertThrows(BusinessRuleException.class,
                () -> reading(new BigDecimal("1.0001"), VehicleReadingUnit.KILOMETER));
    }

    @Test
    void requiresCorrectionReferenceAndReasonTogether() {
        assertThrows(BusinessRuleException.class, () -> new VehicleReading(UUID.randomUUID(), UUID.randomUUID(),
                VehicleReadingType.ODOMETER, BigDecimal.ONE.setScale(3), VehicleReadingUnit.KILOMETER, 0,
                VehicleReadingSourceType.MANUAL, null, NOW, NOW, UUID.randomUUID(), UUID.randomUUID(), null,
                "key", null, NOW));
    }

    private VehicleReading reading(BigDecimal value, VehicleReadingUnit unit) {
        return new VehicleReading(UUID.randomUUID(), UUID.randomUUID(), VehicleReadingType.ODOMETER, value, unit, 0,
                VehicleReadingSourceType.MANUAL, null, NOW, NOW, UUID.randomUUID(), null, null, "key", null, NOW);
    }
}
